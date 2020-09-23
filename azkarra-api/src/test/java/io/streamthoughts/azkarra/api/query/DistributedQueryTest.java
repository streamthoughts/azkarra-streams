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
package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.InMemoryKeyValueIterator;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.query.internal.KeyValueQueryBuilder;
import io.streamthoughts.azkarra.api.query.internal.PreparedQuery;
import io.streamthoughts.azkarra.api.query.internal.Query;
import io.streamthoughts.azkarra.api.query.internal.QueryBuilder;
import io.streamthoughts.azkarra.api.query.result.GlobalResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.ServerHostInfo;
import io.streamthoughts.azkarra.api.streams.ServerMetadata;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DistributedQueryTest {

    public static final String STORE_NAME = "STORE_NAME";

    public static final String REMOTE_SERVER = "remote:1234";

    private RemoteQueryClient client;

    private DistributedQuery<String, Long> distributed;

    private KafkaStreamsContainer streams;

    @BeforeEach
    public void setUp() {
        client = mock(RemoteQueryClient.class);
        streams = mock(KafkaStreamsContainer.class);
        when(streams.isRunning()).thenReturn(true);
        when(streams.defaultKeySerde())
            .thenReturn(Optional.empty());

        when(client.query(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(
                new QueryResultBuilder<>()
                .setServer(REMOTE_SERVER)
                .setStatus(QueryStatus.SUCCESS)
                .setSuccessResultSet(Collections.singletonList(
                    new SuccessResultSet<>(REMOTE_SERVER, true, Collections.singletonList(KV.of("key", 42L)) )))
                .build()
            ));
    }

    @Test
    public void shouldQueryLocalKVStateStoreGivenKeyQuery() {
        distributed = new DistributedQuery<>(client, buildKeyValueQuery());
        when(streams.applicationServer()).thenReturn("local:1234");
        ServerMetadata localServer = newServerMetadata("local", true);
        when(streams.localServerMetadata())
            .thenReturn(Optional.of(localServer));

        when(streams.findMetadataForStoreAndKey(any(), any(), any()))
            .thenReturn(Optional.of(newKeyQueryMetadata("local")));

        ReadOnlyKeyValueStore store = mock(ReadOnlyKeyValueStore.class);
        when(store.get("key")).thenReturn(42L);
        when(streams.localKeyValueStore(matches(STORE_NAME))).thenReturn(new LocalStoreAccessor<>(() -> store));
        when(streams.isSameHost(new HostInfo("local", 1234))).thenReturn(true);

        QueryResult<String, Long> result = distributed.query(streams, Queried.immediately());
        assertNotNull(result);
        assertEquals(QueryStatus.SUCCESS, result.getStatus());
        assertEquals(localServer.hostAndPort(), result.getServer());

        GlobalResultSet<String, Long> rs = result.getResult();
        List<SuccessResultSet<String, Long>> success = rs.getSuccess();
        assertEquals(1, success.size());
        assertEquals(1, success.get(0).getTotal());
        assertEquals(42L, success.get(0).getRecords().get(0).value());
    }

    @Test
    public void shouldQueryRemoteKVStateStoreGivenKeyQuery() {
        distributed = new DistributedQuery<>(client, buildKeyValueQuery());
        when(streams.applicationServer()).thenReturn("local:1234");
        when(streams.localServerMetadata())
            .thenReturn(Optional.of(newServerMetadata("local", true)));

        when(streams.findMetadataForStoreAndKey(any(), any(), any()))
            .thenReturn(Optional.of(newKeyQueryMetadata("remote")));
        when(streams.isSameHost(new HostInfo("remote", 1234))).thenReturn(false);

        QueryResult<String, Long> result = distributed.query(streams, Queried.immediately());
        assertNotNull(result);
        assertEquals(QueryStatus.SUCCESS, result.getStatus());
        assertEquals(newServerMetadata("local", true).hostAndPort(), result.getServer());

        GlobalResultSet<String, Long> rs = result.getResult();
        List<SuccessResultSet<String, Long>> success = rs.getSuccess();
        assertEquals(1, success.size());
        assertEquals(1, success.get(0).getTotal());
        assertEquals("remote:1234", success.get(0).getServer());
        assertEquals(42L, success.get(0).getRecords().get(0).value());
    }

    @Test
    public void shouldQueryLocalAndRemoteKVStateStoreGivenAllQuery() {
        Query<String, Long> all = new QueryBuilder(STORE_NAME).keyValue().all();
        PreparedQuery<String, Long> query = all.prepare();
        distributed = new DistributedQuery<>(client, query);
        when(streams.applicationServer()).thenReturn("local:1234");
        when(streams.localServerMetadata())
            .thenReturn(Optional.of(newServerMetadata("local", true)));

        when(streams.allMetadataForStore(any()))
            .thenReturn(Arrays.asList(
                newServerMetadata("local", true),
                newServerMetadata("remote", false))
            );

        ReadOnlyKeyValueStore store = mock(ReadOnlyKeyValueStore.class);
        when(store.all()).thenReturn(new InMemoryKeyValueIterator<>("key", 42L));
        when(streams.localKeyValueStore(matches(STORE_NAME))).thenReturn(new LocalStoreAccessor<>(() -> store));

        QueryResult<String, Long> result = distributed.query(streams, Queried.immediately());
        assertNotNull(result);
        assertEquals(QueryStatus.SUCCESS, result.getStatus());
        assertEquals(newServerMetadata("local", true).hostAndPort(), result.getServer());

        GlobalResultSet<String, Long> rs = result.getResult();
        List<SuccessResultSet<String, Long>> success = rs.getSuccess();
        assertEquals(2, success.size());
        assertEquals(1, success.get(0).getTotal());
        assertEquals(1, success.get(0).getTotal());
        assertEquals(42L, success.get(0).getRecords().get(0).value());
        assertEquals(42L, success.get(1).getRecords().get(0).value());
    }

    private PreparedQuery<String, Long> buildKeyValueQuery() {
        Query<String, Long> query = new QueryBuilder(STORE_NAME).keyValue().get();
        return query.prepare(new QueryParams(new HashMap<>() {{
            put(KeyValueQueryBuilder.QUERY_PARAM_KEY, "key");
        }}));
    }

    public KeyQueryMetadata newKeyQueryMetadata(final String host) {
        return new KeyQueryMetadata(
            new HostInfo(host, 1234),
            Collections.emptySet(),
            0
        );
    }

    private ServerMetadata newServerMetadata(final String host, final boolean isLocal) {
        return new ServerMetadata(
            new ServerHostInfo("app", host, 1234, isLocal),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet());
    }
}