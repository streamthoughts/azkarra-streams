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

import io.streamthoughts.azkarra.api.events.BasicBlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.BlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.DelegateBlockingRecordQueue;
import io.streamthoughts.azkarra.api.events.EventStream;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.time.SystemTime;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Flow;

public class AsyncMulticastEventStreamPublisherVerificationTest extends FlowPublisherVerification<KV<String, Long>> {

    public AsyncMulticastEventStreamPublisherVerificationTest() {
        super(new TestEnvironment(200));
    }

    @Override
    public Flow.Publisher<KV<String, Long>> createFlowPublisher(long elements) {
        var queue = new AutoCloseBlockingRecordQueue<String, Long>(new BasicBlockingRecordQueue<>(Integer.MAX_VALUE));
        //var queue = new BasicBlockingRecordQueue<String, Long>(Integer.MAX_VALUE);
        EventStream<String, Long> stream = new EventStream<>("test", queue);
        var publisher = new AsyncMulticastEventStreamPublisher<>(stream);
        if (elements < Integer.MAX_VALUE) {
            for (long l = 0; l < elements; l++) {
                stream.send(KV.of("key", l));
            }
        }
        return publisher;
    }

    @Override
    public Flow.Publisher<KV<String, Long>> createFailedFlowPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public long boundedDepthOfOnNextAndRequestRecursion() {
        return 1;
    }

    public static class AutoCloseBlockingRecordQueue<K, V> extends DelegateBlockingRecordQueue<K, V> {


        AutoCloseBlockingRecordQueue(final BlockingRecordQueue<K, V> delegate) {
            super(delegate);
        }

        @Override
        public KV<K, V> poll(Duration timeout) throws InterruptedException {
            KV<K, V> r = super.poll(timeout);
            mayScheduleClose();
            return r;
        }

        @Override
        public KV<K, V> poll() {
            KV<K, V> r = super.poll();
            mayScheduleClose();
            return r;
        }

        @Override
        public void drainTo(Collection<? super KV<K, V>> collection) {
            super.drainTo(collection);
        }

        private void mayScheduleClose() {
            if (isEmpty()) {
                new Thread(() -> {
                    SystemTime.SYSTEM.sleep(Duration.ofMillis(100));
                    close();
                }).start();
            }
        }
    }
}