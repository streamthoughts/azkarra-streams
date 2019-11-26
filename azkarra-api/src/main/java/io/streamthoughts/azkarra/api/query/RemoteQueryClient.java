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

import io.streamthoughts.azkarra.api.streams.StreamsServerInfo;
import io.streamthoughts.azkarra.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;

/**
 * Default interface to query a remote streams state store.
 */
public interface RemoteQueryClient {


    /**
     * Executes a query to the specified server.
     *
     * @param server     the {@link StreamsServerInfo} to query.
     * @param query      the {@link QueryInfo} to send.
     * @param options    the {@link Queried} options.
     *
     * @return  a {@link CompletableFuture} of {@link QueryResult}.
     */
    <K, V> CompletableFuture<QueryResult<K, V>> query(final StreamsServerInfo server,
                                                      final QueryInfo query,
                                                      final Queried options);
}
