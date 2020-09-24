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

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.http.sse.ServerSentEventSubscriber;
import io.streamthoughts.azkarra.serialization.json.Json;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import org.mockito.Mockito;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;

import java.util.concurrent.Flow;

public class SSEFlowSubscriberBlackboxVerificationTest extends FlowSubscriberBlackboxVerification<KV<String, Integer>> {

    protected SSEFlowSubscriberBlackboxVerificationTest() {
        super(new TestEnvironment(200));
    }

    @Override
    public Flow.Subscriber<KV<String, Integer>> createFlowSubscriber() {
        return new ServerSentEventSubscriber<>(
            Mockito.mock(ServerSentEventConnection.class),
            "test",
            "app",
            Json.getDefault()
        );
    }

    @Override
    public KV<String, Integer> createElement(int elements) {
        return KV.of("key", elements);
    }
}
