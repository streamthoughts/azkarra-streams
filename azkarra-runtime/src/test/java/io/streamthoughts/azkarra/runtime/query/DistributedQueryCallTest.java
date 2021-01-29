/*
 * Copyright 2019-2021 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.query;

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.query.GenericQueryParams;
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalPreparedQuery;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.internal.QueryBuilder;
import io.streamthoughts.azkarra.api.query.result.GlobalResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsInstance;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsMetadata;
import io.streamthoughts.azkarra.api.util.Endpoint;
import io.streamthoughts.azkarra.runtime.streams.LocalKafkaStreamsContainer;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistributedQueryCallTest {

    private static final String STORE_NAME = "STORE_NAME";
    private static final Endpoint LOCAL_ENDPOINT = new Endpoint("local", 1234);
    private static final Endpoint REMOTE_ENDPOINT = new Endpoint("remote", 1234);

    private RemoteStateStoreClient client;

    private DistributedQueryCall<String, Long> call;

    private LocalKafkaStreamsContainer container;

    @BeforeEach
    public void setUp() {
        client = mock(RemoteStateStoreClient.class);
        container = mock(LocalKafkaStreamsContainer.class);
        when(container.isRunning()).thenReturn(true);
        when(container.endpoint()).thenReturn(Optional.of(LOCAL_ENDPOINT));
        when(container.defaultKeySerde())
            .thenReturn(Optional.empty());

        when(client.query(any(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(
                new QueryResultBuilder<>()
                    .setServer(REMOTE_ENDPOINT.toString())
                    .setStatus(QueryStatus.SUCCESS)
                    .setSuccessResultSet(Collections.singletonList(
                            new SuccessResultSet<>(REMOTE_ENDPOINT.toString(), true, Collections.singletonList(KV.of("key", 42L)) )))
                    .build()
            ));
    }

    @Test
    public void shouldQueryLocalKVStateStoreGivenKeyQuery() {
        call = new DistributedQueryCall<>(newLocalExecutableQuery(), container, client);
        when(container.describe()).thenReturn(newKafkaStreamsInstance(LOCAL_ENDPOINT, true));

        when(container.findMetadataForStoreAndKey(any(), any(), any()))
                .thenReturn(Optional.of(newKeyQueryMetadata("local")));

        ReadOnlyKeyValueStore store = mock(ReadOnlyKeyValueStore.class);
        when(store.get("key")).thenReturn(42L);
        when(container.localKeyValueStore(matches(STORE_NAME))).thenReturn(new LocalStoreAccessor<>(() -> store));
        when(container.checkEndpoint(new Endpoint("local", 1234))).thenReturn(true);

        QueryResult<String, Long> result = call.execute(QueryOptions.immediately());
        assertNotNull(result);
        assertEquals(QueryStatus.SUCCESS, result.getStatus());
        assertEquals(LOCAL_ENDPOINT.toString(), result.getServer());

        GlobalResultSet<String, Long> rs = result.getResult();
        List<SuccessResultSet<String, Long>> success = rs.getSuccess();
        assertEquals(1, success.size());
        assertEquals(1, success.get(0).getTotal());
        assertEquals(42L, success.get(0).getRecords().get(0).value());
    }

    @Test
    public void shouldQueryRemoteKVStateStoreGivenKeyQuery() {
        call = new DistributedQueryCall<>(newLocalExecutableQuery(), container, client);
        when(container.findMetadataForStoreAndKey(any(), any(), any()))
                .thenReturn(Optional.of(newKeyQueryMetadata("remote")));
        when(container.checkEndpoint(new Endpoint("remote", 1234))).thenReturn(false);

        QueryResult<String, Long> result = call.execute(QueryOptions.immediately());
        assertNotNull(result);
        assertEquals(QueryStatus.SUCCESS, result.getStatus());
        assertEquals(LOCAL_ENDPOINT.toString(), result.getServer());

        GlobalResultSet<String, Long> rs = result.getResult();
        List<SuccessResultSet<String, Long>> success = rs.getSuccess();
        assertEquals(1, success.size());
        assertEquals(1, success.get(0).getTotal());
        assertEquals(REMOTE_ENDPOINT.toString(), success.get(0).getServer());
        assertEquals(42L, success.get(0).getRecords().get(0).value());
    }

    @Test
    public void shouldQueryLocalAndRemoteKVStateStoreGivenAllQuery() {
        LocalPreparedQuery<String, Long> all = new QueryBuilder(STORE_NAME).keyValue().all();
        LocalExecutableQuery<String, Long> query = all.compile(new GenericQueryParams());
        call = new DistributedQueryCall<>(query, container, client);
        when(container.describe()).thenReturn(newKafkaStreamsInstance(LOCAL_ENDPOINT, true));
        when(container.findAllEndpointsForStore(any()))
            .thenReturn(new HashSet<>() {{
                add(LOCAL_ENDPOINT);
                add(REMOTE_ENDPOINT);
            }});

        ReadOnlyKeyValueStore store = mock(ReadOnlyKeyValueStore.class);
        when(store.all()).thenReturn(new InMemoryKeyValueIterator<>("key", 42L));
        when(container.localKeyValueStore(matches(STORE_NAME))).thenReturn(new LocalStoreAccessor<>(() -> store));

        QueryResult<String, Long> result = call.execute(QueryOptions.immediately());
        assertNotNull(result);
        assertEquals(QueryStatus.SUCCESS, result.getStatus());
        assertEquals(LOCAL_ENDPOINT.toString(), result.getServer());

        GlobalResultSet<String, Long> rs = result.getResult();
        List<SuccessResultSet<String, Long>> success = rs.getSuccess();
        assertEquals(2, success.size());
        assertEquals(1, success.get(0).getTotal());
        assertEquals(1, success.get(0).getTotal());
        assertEquals(42L, success.get(0).getRecords().get(0).value());
        assertEquals(42L, success.get(1).getRecords().get(0).value());
    }

    private KafkaStreamsInstance newKafkaStreamsInstance(final Endpoint endpoint, final boolean isLocal) {
        return new KafkaStreamsInstance(
                "test",
                endpoint,
                isLocal,
                KafkaStreamsMetadata.EMPTY
        );
    }

    private LocalExecutableQuery<String, Long> newLocalExecutableQuery() {
        final LocalPreparedQuery<String, Long> preparedQuery = QueryBuilder.store(STORE_NAME)
                .keyValue()
                .get();
        final GenericQueryParams params = new GenericQueryParams();
        params.put(QUERY_PARAM_KEY, "key");
        return preparedQuery.compile(params);
    }

    public KeyQueryMetadata newKeyQueryMetadata(final String host) {
        return new KeyQueryMetadata(
                new HostInfo(host, 1234),
                Collections.emptySet(),
                0
        );
    }

    /**
     * In-memory {@link KeyValueIterator} which can be used for testing purpose.
     *
     * @param <K>   the key type.
     * @param <V>   the value type.
     */
    public static class InMemoryKeyValueIterator <K, V> implements KeyValueIterator<K, V> {

        private final Iterator<KeyValue<K, V>> iterator;

        /**
         * Creates a new {@link InMemoryKeyValueIterator} for the specified key-value pair.
         *
         * @param key       the record key.
         * @param value     the record value.
         */
        public InMemoryKeyValueIterator(final K key, final V value)  {
            this.iterator = Collections.singletonList(KeyValue.pair(key, value)).iterator();
        }

        @Override
        public void close() {
        }

        @Override
        public K peekNextKey() {
            return null;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public KeyValue<K, V> next() {
            return iterator.next();
        }
    }
}