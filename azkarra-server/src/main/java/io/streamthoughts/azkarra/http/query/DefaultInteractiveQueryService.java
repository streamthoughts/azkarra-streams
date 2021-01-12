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

package io.streamthoughts.azkarra.http.query;

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsStateException;
import io.streamthoughts.azkarra.api.query.DistributedQuery;
import io.streamthoughts.azkarra.api.query.InteractiveQueryService;
import io.streamthoughts.azkarra.api.query.Queried;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.RemoteQueryClient;
import io.streamthoughts.azkarra.api.query.internal.Query;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.time.Time;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DefaultInteractiveQueryService implements InteractiveQueryService {

    private final RemoteQueryClient client;
    private final AzkarraStreamsService service;

    /**
     * Creates a new {@link DefaultInteractiveQueryService} instance.
     * @param client    the {@link RemoteQueryClient} instance.
     */
    public DefaultInteractiveQueryService(final AzkarraStreamsService service,
                                          final RemoteQueryClient client) {
        this.service = service;
        this.client = Objects.requireNonNull(client, "client cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> QueryResult<K, V> query(final String applicationId,
                                          final Query<K, V> query,
                                          final QueryParams parameters,
                                          final Queried options) {
        final long now = Time.SYSTEM.milliseconds();

        final KafkaStreamsContainer container = service.getStreamsById(applicationId);

        checkIsRunning(container);

        final Optional<List<Error>> errors = query.validate(parameters);
        if (errors.isPresent()) {
            QueryResultBuilder<K, V> queryBuilder = QueryResultBuilder.newBuilder();
            final String server = container.applicationServer();
            return queryBuilder
                .setServer(server)
                .setTook(Time.SYSTEM.milliseconds() - now)
                .setStatus(QueryStatus.INVALID)
                .setFailedResultSet(new ErrorResultSet(server, false, QueryError.allOf(errors.get())))
                .build();
        }

        final DistributedQuery<K, V> distributed = new DistributedQuery<>(client, query.prepare(parameters));
        return distributed.query(container, options);
    }

    private void checkIsRunning(final KafkaStreamsContainer streams) {
        if (!streams.isRunning()) {
            throw new InvalidStreamsStateException(
                    "streams instance for id '" + streams.applicationId() +
                            "' is not running (" + streams.state().value() + ")"
            );
        }
    }
}
