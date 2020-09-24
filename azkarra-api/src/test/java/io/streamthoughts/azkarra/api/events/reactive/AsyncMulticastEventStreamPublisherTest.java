/*
 * Copyright 2019-2020 StreamThoughts.
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

import io.streamthoughts.azkarra.api.events.BasicBlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.time.SystemTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Flow;

public class AsyncMulticastEventStreamPublisherTest {

    private static final int DEFAULT_TIMEOUT_MS = 200;

    @Test
    public void test(){
        AsyncMulticastEventStreamPublisher<String, Long> publisher = createPublisher(5);
        CaptureSubscriber<String, Long> subscriber = new CaptureSubscriber<>();
        publisher.subscribe(subscriber);
        waitAndAssertReceived(subscriber, 0);
        subscriber.subscription.request(1);
        waitAndAssertReceived(subscriber, 1);
        subscriber.subscription.request(1);
        subscriber.subscription.request(2);
        waitAndAssertReceived(subscriber, 4);
    }

    private void waitAndAssertReceived(final CaptureSubscriber<String, Long> subscriber, int expected) {
        SystemTime.SYSTEM.sleep(Duration.ofMillis(DEFAULT_TIMEOUT_MS));
        Assertions.assertEquals(expected, subscriber.received.size());
    }

    private AsyncMulticastEventStreamPublisher<String, Long> createPublisher(long elements) {
        var queue = new BasicBlockingRecordQueue<String, Long>(Integer.MAX_VALUE);
        EventStream<String, Long> stream = new EventStream<>("test", queue);
        var publisher = new AsyncMulticastEventStreamPublisher<>(stream);
        if (elements < Integer.MAX_VALUE) {
            for (long l = 0; l < elements; l++) {
                stream.send(KV.of("key", l));
            }
        }
        return publisher;
    }

    public class CaptureSubscriber<K, V> implements Flow.Subscriber<KV<K, V>> {

        List<KV<K, V>> received = new LinkedList<>();
        Flow.Subscription subscription;
        Throwable throwable;
        boolean complete = false;

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(final KV<K, V> record) {
            received.add(record);
        }

        @Override
        public void onError(final Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onComplete() {
            complete = true;
        }
    }
}