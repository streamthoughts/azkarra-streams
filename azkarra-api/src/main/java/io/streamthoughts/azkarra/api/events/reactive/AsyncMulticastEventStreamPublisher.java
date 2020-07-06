/*
 * Copyright 2020 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.azkarra.api.events.reactive;

import io.streamthoughts.azkarra.api.events.BlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.events.callback.QueueCallback;
import io.streamthoughts.azkarra.api.events.reactive.internal.SequentialSubscriptionIdGenerator;
import io.streamthoughts.azkarra.api.events.reactive.internal.SubscriptionId;
import io.streamthoughts.azkarra.api.events.reactive.internal.SubscriptionIdGenerator;
import io.streamthoughts.azkarra.api.model.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * The AsyncMulticastEventStreamPublisher is an implementation of Reactive Streams {@link Flow.Publisher}
 * which executes asynchronously, using an internal single-thread {@link java.util.concurrent.Executor}.
 *
 * Records are produced from a given {@link EventStream} in a "multicast" configuration to its {@link Flow.Subscriber}.
 *
 * Note: A subscriber will start receiving events from the head of the event-stream buffer,
 * as soon as it perform a valid request demand. The publisher will continue to poll the event-stream buffer as long
 * as at-least one subscriber is requesting more records, i.e even if other subscriptions are applying back-pressure.
 *
 * @since 0.8.0
 */
public class AsyncMulticastEventStreamPublisher<K, V> implements EventStreamPublisher<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncMulticastEventStreamPublisher.class);

    // The maximum number of records to try to poll from the queue on each send.
    private static final int MAX_POLL_RECORDS = 100;
    private static final Duration AWAIT_TERMINATION_TIMEOUT = Duration.ofSeconds(30);

    private final EventStream<K, V> stream;
    private final SubscriptionIdGenerator idGenerator;

    private final EventLoop eventLoop = new EventLoop();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private BlockingRecordQueue<K, V> queue;

    private final ConcurrentHashMap<SubscriptionId, EventStreamSubscription<KV<K, V>>> subscriptions
            = new ConcurrentHashMap<>();

    /**
     * Creates a new {@link AsyncMulticastEventStreamPublisher} instance.
     *
     * @param stream   the {@link EventStream}.
     */
    public AsyncMulticastEventStreamPublisher(final EventStream<K, V> stream) {
        this(stream, new SequentialSubscriptionIdGenerator());
    }

    /**
     * Creates a new {@link AsyncMulticastEventStreamPublisher} instance.
     *
     * @param stream   the {@link EventStream}.
     */
    private AsyncMulticastEventStreamPublisher(final EventStream<K, V> stream,
                                               final SubscriptionIdGenerator subscriptionIdGenerator) {
        this.stream = requireNonNull(stream, "stream cannot be null");
        this.idGenerator = requireNonNull(subscriptionIdGenerator, "subscriptionIdGenerator cannot be null");
        init();
    }

    private void init() {
        stream.open(queue -> {
            AsyncMulticastEventStreamPublisher.this.queue = queue;
            queue.setQueueCallback(new QueueCallback() {
                @Override
                public void onClosed() {
                    closed.set(true);
                    eventLoop.signal(Complete.Instance);
                    eventLoop.awaitTerminationAndDoComplete(AWAIT_TERMINATION_TIMEOUT);
                }

                @Override
                public void onQueued() {
                    eventLoop.signal(Send.Instance);
                }
            });
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return stream.type();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(final Flow.Subscriber<? super KV<K, V>> subscriber) {
        // throw NullPointerException if an illegal parameter is passed (rule 2.13)
        Objects.requireNonNull(subscriber, "subscriber cannot be null");

        if (closed.get()) {
            // make sure we signal onSubscribe before onComplete,
            // because the publisher is already closed we do not have anything to send
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override public void cancel() {}
                @Override public void request(long n) {}
            });
            subscriber.onComplete();
            return;
        }

        new InternalEventStreamSubscription(subscriber, idGenerator.generateNext()).init();
    }

    private boolean hasMoreRecords() {
        return !queue.isEmpty();
    }

    private boolean hasActiveSubscriptions() {
        return subscriptions.values()
            .stream()
            .anyMatch(EventStreamSubscription::canReceived);
    }

    private Collection<EventStreamSubscription<KV<K, V>>> subscriptions() {
        return subscriptions.values();
    }

    // The MulticastEventStreamPublisher protocol
    interface Signal { }
    enum Send implements Signal { Instance } // Multi-cast
    enum Complete implements Signal { Instance } // Multi-cast
    static abstract class SubscriptionSignal implements Signal {
        final EventStreamSubscription subscription;

        SubscriptionSignal(final EventStreamSubscription subscription) {
            this.subscription = Objects.requireNonNull(subscription, "subscription cannot be null");
        }

        public abstract void execute();
    }
    static final class Request<K, V> extends SubscriptionSignal {
        final long demands;

        Request(final EventStreamSubscription<KV<K, V>> subscription, final long demands) {
            super(subscription);
            this.demands = demands;
        }

        @Override
        public void execute() {
            subscription.doOnRequest(demands);
        }
    }

    static final class Cancel<K, V> extends SubscriptionSignal {
        Cancel(final EventStreamSubscription<KV<K, V>> subscription) {
            super(subscription);
        }

        @Override
        public void execute() {
            subscription.doOnCancel();
        }
    }

    static final class Subscribe<K, V> extends SubscriptionSignal {
        Subscribe(final EventStreamSubscription<KV<K, V>> subscription) {
            super(subscription);
        }

        @Override
        public void execute() {
            subscription.doOnSubscribe();
        }
    }

    /**
     * The main event-loop that processes {@link Signal}.
     */
    private class EventLoop implements Runnable {

        private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<>();

        private ExecutorService executor;

        private final AtomicBoolean on = new AtomicBoolean(false);

        /**
         * Creates a new {@link EventLoop}.
         */
        EventLoop() {
            executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "event-streams-loop-publisher"));
        }

        public void signal(final Signal signal) {
            if (inboundSignals.offer(signal))
                tryScheduleToExecute();
        }

        private void tryScheduleToExecute() {
            if(on.compareAndSet(false, true)) {
                try {
                    executor.execute(this);
                } catch (Throwable exception) {
                    // The publisher is already closed or is
                    if (closed.get()) {
                        on.set(false);
                    }
                }
            }
        }

        @Override
        public void run() {
            if (on.get()) {
                try {
                    final Signal s = inboundSignals.poll();
                    if (s != null) {
                        LOG.trace("processing: {}", s.getClass().getSimpleName());
                        if (s == Send.Instance) {
                            maySendNextRecords();
                        }
                        if (s == Complete.Instance) {
                            try {
                                mayDrainAllRecords();
                            } finally {
                                shutdown();
                                inboundSignals.clear(); // do not process any signals after shutdown
                            }
                        }
                        if (s instanceof SubscriptionSignal) {
                            ((SubscriptionSignal)s).execute();
                        }
                    }
                } finally {
                    on.set(false);
                    // do proceed if the last signal was not `Complete`
                    if (!executor.isShutdown() && !inboundSignals.isEmpty()) {
                        tryScheduleToExecute();
                    }
                }
            }
        }

        private void shutdown() {
            LOG.info("Shutting down event-loop");
            executor.shutdown();
        }

        void awaitTerminationAndDoComplete(final Duration forceAfterTimeout) {
            try {
                executor.awaitTermination(forceAfterTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                executor.shutdownNow();
                LOG.error("EventLoop failed to terminate before timeout, unsent events: {}", queue.size());
            } finally {
                subscriptions().forEach(EventStreamSubscription::doOnComplete);
            }
        }

        private void mayDrainAllRecords() {
            if (hasActiveSubscriptions() && hasMoreRecords()) {
                // Try to send all remaining records before completing subscriptions.
                Collection<KV<K, V>> records = new LinkedList<>();
                queue.drainTo(records);
                records.forEach(record -> {
                    subscriptions().forEach(subscription -> {
                        if (subscription.canReceived())
                            subscription.doOnNext(record);
                    });
                });
            }
        }

        private void maySendNextRecords() {
            int sent = 0;
            while (hasMoreRecords() && sent < MAX_POLL_RECORDS) {
                // return immediately if no subscription can receive more records.
                if (!hasActiveSubscriptions()) {
                    LOG.trace("no subscription ready for next records. Signal ignored.");
                    break;
                }
                KV<K, V> record = queue.poll();
                subscriptions().forEach(subscription -> {
                    if (subscription.canReceived())
                        subscription.doOnNext(record);
                });
                sent++;
            }
        }
    }

    interface EventStreamSubscription<T> extends Flow.Subscription {
        // MUST be executed only from the event-loop
        void doOnNext(final T record);
        // MUST be executed only from the event-loop
        void doOnSubscribe();
        // MUST be executed only from the event-loop
        boolean canReceived();
        // MUST be executed only from the event-loop
        void doOnRequest(final long demands);
        void doOnComplete();
        void doOnCancel();
    }

    /**
     * The internal {@link EventStreamSubscription} implementation.
     */
    private class InternalEventStreamSubscription implements EventStreamSubscription<KV<K, V>> {

        private final Flow.Subscriber<? super KV<K, V>> subscriber;

        private final SubscriptionId subscriptionId;

        private long demands = 0L;

        private boolean completed = false;

        private boolean done = false;

        InternalEventStreamSubscription(final Flow.Subscriber<? super KV<K, V>> subscriber,
                                        final SubscriptionId subscriptionId) {
            this.subscriber = subscriber;
            this.subscriptionId = subscriptionId;
        }

        @Override
        public boolean canReceived() {
            return demands > 0 && !done;
        }

        @Override
        public void doOnRequest(final long n) {
            if (n < 1) { // rule 3.9
                subscriber.onError(new IllegalArgumentException("non-positive request signals are illegal"));
                return;
            }

            if (demands + n < 1) {
                demands = Long.MAX_VALUE;
            } else {
                demands += n;
            }
            eventLoop.signal(Send.Instance);
        }

        private void decrementRequested() {
            if (demands != Long.MAX_VALUE) // rule 3.17, i.e. unbounded
                demands--;
        }

        @Override
        public void doOnSubscribe() {
            subscriptions.put(subscriptionId, this);
            subscriber.onSubscribe(this);
        }

        @Override
        public void doOnComplete() {
            if (completed) return;

            try {
                doOnCancel(); // rule 1.6 - consider the Subscription cancelled when onComplete is signalled
                subscriber.onComplete();
                completed = true;
            } catch (Exception e) {
                LOG.error("Exception occurred while calling onComplete", e);
            }
        }

        @Override
        public void doOnCancel() {
            subscriptions.remove(subscriptionId); // drop any reference to this subscription/subscriber
            done = true;
        }

        @Override
        public void doOnNext(final KV<K, V> record) {
            try {
                subscriber.onNext(record);
                decrementRequested();
            } catch (Exception e) {
                LOG.error("Exception occurred while calling onNext", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void request(final long demands) {
            if (completed) return; // rule 3.6
            eventLoop.signal(new Request<>(this, demands));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void cancel() {
            if (done) return; // rule 3.7

            // async help with rule 3.4
            eventLoop.signal(new Cancel<>(this));
        }

        void init() {
            eventLoop.signal(new Subscribe<>(this));
        }
    }
}
