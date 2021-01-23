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

import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.runtime.streams.LocalKafkaStreamsContainer;

import java.util.Objects;

public class DistributedQueryExecutionDelegatee implements QueryExecutionDelegatee {

    private final RemoteStateStoreClient client;

    /**
     * Creates a new {@link DistributedQueryExecutionDelegatee} instance.
     *
     * @param client    the {@link RemoteStateStoreClient}.
     */
    public DistributedQueryExecutionDelegatee(final RemoteStateStoreClient client) {
        this.client = Objects.requireNonNull(client, "client should not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends KafkaStreamsContainer> supportedClass() {
        return LocalKafkaStreamsContainer.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> QueryResult<K, V> execute(final KafkaStreamsContainer container,
                                            final QueryRequest queryRequest,
                                            final QueryOptions queryOptions) {
        final LocalExecutableQuery<K, V> compiled = queryRequest.compile();
        final LocalKafkaStreamsContainer localContainer = (LocalKafkaStreamsContainer) container;

        return new DistributedQueryCall<>(compiled, localContainer, client).execute(queryOptions);
    }
}
