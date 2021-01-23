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

import io.streamthoughts.azkarra.api.AzkarraStreamsService;
import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsStateException;
import io.streamthoughts.azkarra.api.query.InteractiveQueryService;
import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.time.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@code DefaultInteractiveQueryService} is used to execute a query request.
 *
 * The query execution can be delegate to a {@link QueryExecutionDelegatee}.
 */
public class DefaultInteractiveQueryService implements InteractiveQueryService {

    private final AzkarraStreamsService service;
    private final List<QueryExecutionDelegatee> executors;

    /**
     * Creates a new {@link DefaultInteractiveQueryService} instance.
     *
     * @param service    the {@link AzkarraStreamsService} instance.
     */
    public DefaultInteractiveQueryService(final AzkarraStreamsService service) {
        this(service, new ArrayList<>());
    }

    /**
     * Creates a new {@link DefaultInteractiveQueryService} instance.
     *
     * @param service    the {@link AzkarraStreamsService} instance.
     */
    public DefaultInteractiveQueryService(final AzkarraStreamsService service,
                                          final List<QueryExecutionDelegatee> executors) {
        this.executors = Objects.requireNonNull(executors, "executors should not be null");
        this.service = Objects.requireNonNull(service, "service should not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> QueryResult<K, V> execute(final String applicationId,
                                            final QueryRequest queryRequest,
                                            final QueryOptions queryOptions) {
        final long now = Time.SYSTEM.milliseconds();

        final KafkaStreamsContainer container = service.getStreamsById(applicationId);

        checkIsRunning(container);

        // Validate the query so that we can build a custom result.
        final Optional<List<Error>> errors = queryRequest.validate();

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

        // Check if we can find an executor to delegate the execution.
        final Optional<QueryExecutionDelegatee> candidate = executors.stream()
                .filter(delegatee -> delegatee.supportedClass().isAssignableFrom(container.getClass()))
                .findFirst();

        if (candidate.isPresent()) {
            final QueryExecutionDelegatee handler = candidate.get();
            return handler.execute(container, queryRequest, queryOptions);
        }

        // Otherwise, execute the query directly.
        final QueryCall<K, V> call = container.newQueryCall(queryRequest);
        return call.execute(queryOptions);
    }

    /**
     * Registers a new {@link QueryExecutionDelegatee} instance.
     *
     * @param delegatee the {@link QueryExecutionDelegatee} to register.
     */
    public void registerQueryExecutionDelegatee(final QueryExecutionDelegatee delegatee) {
        this.executors.add(Objects.requireNonNull(delegatee, "delegatee cannot be null"));
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
