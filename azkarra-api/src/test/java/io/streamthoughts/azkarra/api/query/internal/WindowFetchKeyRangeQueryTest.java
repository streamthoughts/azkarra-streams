/*
 * Copyright 2019 StreamThoughts.
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
package io.streamthoughts.azkarra.api.query.internal;

import io.streamthoughts.azkarra.api.InMemoryKeyValueIterator;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WindowFetchKeyRangeQueryTest {

    static final String STORE_NAME = "storeName";
    static final String KEY_FROM = "keyFrom";
    static final String KEY_TO = "keyTo";

    @Test
    public void shouldFetchKeyRange() {
        WindowFetchKeyRangeQuery<String, String> query = new WindowFetchKeyRangeQuery<>(
            STORE_NAME, KEY_FROM, KEY_TO, Instant.MIN, Instant.MAX);
        KafkaStreamsContainer mkContainer = Mockito.mock(KafkaStreamsContainer.class);


        Windowed<String> windowed = new Windowed<>(KEY_FROM, new TimeWindow(0L, 1L));
        ReadOnlyWindowStore store = mock(ReadOnlyWindowStore.class);
        when(store.fetch(KEY_FROM, KEY_TO, Instant.MIN, Instant.MAX)).thenReturn(
                new InMemoryKeyValueIterator<>(windowed, "value")
        );
        when(mkContainer.getLocalWindowStore(matches(STORE_NAME))).thenReturn(new LocalStoreAccessor<>(() -> store));

        Try<List<KV<Windowed<String>, String>>> result = query.execute(mkContainer);
        Assertions.assertEquals(1, result.get().size());
        Assertions.assertEquals(KV.of(windowed, "value"), result.get().get(0));
    }
}