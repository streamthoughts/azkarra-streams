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

import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsStateException;
import io.streamthoughts.azkarra.api.monad.Either;
import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalExecutableQueryWithKey;
import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.api.time.Time;
import io.streamthoughts.azkarra.api.util.Endpoint;
import io.streamthoughts.azkarra.api.util.FutureCollectors;
import io.streamthoughts.azkarra.runtime.streams.LocalKafkaStreamsContainer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.runtime.query.internal.QueryResultUtils.buildNotAvailableResult;
import static io.streamthoughts.azkarra.runtime.query.internal.QueryResultUtils.buildQueryResult;

public class DistributedQueryCall<K, V> extends BaseAsyncQueryCall<K, V, LocalExecutableQuery<K, V>> {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedQueryCall.class);

    private final LocalKafkaStreamsContainer container;

    private final RemoteQueryCallFactory callFactory;

    /**
     * Creates a new {@link DecorateQuery} instance.
     *
     * @param query the query.
     */
    public DistributedQueryCall(final LocalExecutableQuery<K, V> query,
                                final LocalKafkaStreamsContainer container,
                                final RemoteQueryCallFactory callFactory) {
        super(query);
        this.container = Objects.requireNonNull(container, "container should not be null");
        this.callFactory = Objects.requireNonNull(callFactory, "callFactory should not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<K, V> execute(final QueryOptions options) {
        Objects.requireNonNull(options, "options cannot be null");
        long now = Time.SYSTEM.milliseconds();

        // Quickly check if streams instance is still running
        if (!container.isRunning()) {
            throw new InvalidStreamsStateException(
                    "streams instance for id '" + applicationId() +
                            "' is not running (" + container.state().value() + ")"
            );
        }

        QueryResult<K, V> result;
        if (isKeyedQuery()) {
            result = querySingleHostStateStore(options);
        } else {
            result = queryMultiHostStateStore(options);
        }
        return result
                .server(localEndpointListener())
                .took(Time.SYSTEM.milliseconds() - now);
    }

    private boolean isKeyedQuery() {
        return query instanceof LocalExecutableQueryWithKey;
    }

    private Object getKey() {
        return ((LocalExecutableQueryWithKey) query).getKey();
    }

    private Serializer<Object> getKeySerializer() {
        return ((LocalExecutableQueryWithKey) query).getKeySerializer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryCall<K, V> renew() {
        return new DistributedQueryCall<>(query, container, callFactory);
    }

    /**
     * Execute this key-query either locally or remotely.
     *
     * @param options the {@link QueryOptions} option.
     * @return the {@link QueryResult}
     * @throws AzkarraException A local query can fail if the container is re-initializing (task migration)
     *                          or the store is not initialized (or closed).
     *                          <p>
     *                          A remote query can fail if the remote instance is down and re-balancing has not occurred yet.
     */
    private QueryResult<K, V> querySingleHostStateStore(final QueryOptions options) throws AzkarraException {
        return container
                .findMetadataForStoreAndKey(query.getStoreName(), getKey(), getKeySerializer())
                .map(keyQueryMetadata -> {
                    var activeHost = keyQueryMetadata.getActiveHost();
                    final Endpoint endpoint = new Endpoint(activeHost.host(), activeHost.port());
                    final QueryCall<K, V> call;
                    if (container.checkEndpoint(endpoint)) {
                        call = new LocalQueryCall<>(container, query);
                    } else if (options.remoteAccessAllowed()) {
                        call = createRemoteQueryCall(endpoint);
                    } else {
                        call = new EmptyQueryCall<>(query);
                    }
                    return call.execute(options);
                }).orElseGet(() -> {
                    var error = "no metadata available for store '" + query.getStoreName() + "', key '" + getKey() + "'";
                    return buildNotAvailableResult(error);
                });
    }

    private QueryRequest newQueryRequest() {
        return new QueryRequest(query);
    }

    private String applicationId() {
        return container.applicationId();
    }

    private Endpoint localEndpoint() {
        return container.endpoint().get();
    }

    private String localEndpointListener() {
        return container.endpoint().get().listener();
    }

    private QueryResult<K, V> queryMultiHostStateStore(final QueryOptions options) {

        final List<Either<SuccessResultSet<K, V>, ErrorResultSet>> results = new LinkedList<>();

        var endpoints = container.findAllEndpointsForStore(query.getStoreName());

        if (endpoints.isEmpty()) {
            String error = "no metadata available for store '" + query.getStoreName() + "'";
            LOG.warn(error);
            return buildNotAvailableResult(error);
        }

        List<CompletableFuture<QueryResult<K, V>>> remotes = null;
        if (options.remoteAccessAllowed()) {
            // Forward query to all remote instances
            remotes = endpoints.stream()
                    .filter(Predicate.not(container::checkEndpoint))
                    .map(endpoint -> {
                        final QueryCall<K, V> call = createRemoteQueryCall(endpoint);
                        // disable retries and remote
                        final QueryOptions newOptions = options.withRemoteAccessAllowed(false).withRetries(0);
                        return call.executeAsync(newOptions);
                    })
                    .collect(Collectors.toList());
        }
        //Execute the query locally only if the local instance own the queried store.
        endpoints.stream()
                .filter(container::checkEndpoint)
                .findFirst()
                .map(local -> {
                    final LocalQueryCall<K, V> call = new LocalQueryCall<>(container, query);
                    // disable retries
                    final QueryOptions newOptions = options.withRetries(0);
                    return call.execute(newOptions).getResult().unwrap().get(0);
                })
                .ifPresent(results::add);

        if (remotes != null) {
            // Blocking
            results.addAll(waitRemoteThenGet(remotes));
        }
        return buildQueryResult(results);
    }

    private QueryCall<K, V> createRemoteQueryCall(final Endpoint endpoint) {
        return callFactory.create(applicationId(), localEndpoint(), endpoint, newQueryRequest());
    }

    private static <K, V> List<Either<SuccessResultSet<K, V>, ErrorResultSet>> waitRemoteThenGet(
            final List<CompletableFuture<QueryResult<K, V>>> futures
    ) {

        final CompletableFuture<List<QueryResult<K, V>>> future = futures
                .stream()
                .collect(FutureCollectors.allOf());
        try {
            // futures should never complete exceptionally.
            return future.handle((results, throwable) -> {
                if (results != null) {
                    return results.stream()
                            .map(QueryResult::getResult)
                            .flatMap(o -> o.unwrap().stream())
                            .collect(Collectors.toList());
                }
                LOG.error("This exception should not have happened", throwable);
                // should never happens.
                return Collections.<Either<SuccessResultSet<K, V>, ErrorResultSet>>emptyList();
            }).get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unexpected error happens while waiting for remote query results", e);
            return Collections.emptyList();
        }
    }
}
