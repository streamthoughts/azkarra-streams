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

import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.AzkarraRetriableException;
import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.errors.InvalidStreamsStateException;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Either;
import io.streamthoughts.azkarra.api.monad.Retry;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.internal.PreparedQuery;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.StreamsServerInfo;
import io.streamthoughts.azkarra.api.time.Time;
import io.streamthoughts.azkarra.api.util.FutureCollectors;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Default class to query a state store either locally, remotely or globally.
 */
public class DistributedQuery<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedQuery.class);

    private final RemoteQueryClient remoteQueryClient;

    private final PreparedQuery<K, V> query;

    /**
     * Creates a new {@link DistributedQuery} instance.
     *
     */
    public DistributedQuery(final RemoteQueryClient remoteQueryClient,
                            final PreparedQuery<K, V> query) {
        Objects.requireNonNull(remoteQueryClient, "remoteQueryClient cannot be null");
        Objects.requireNonNull(query, "query cannot be null");
        this.remoteQueryClient = remoteQueryClient;
        this.query = query;
    }

    /**
     * Executes this interactive query for the given {@link org.apache.kafka.streams.KafkaStreams} instance.
     *
     * @param streams    the {@link KafkaStreamsContainer} instance on which to execute this query.
     * @param options    the {@link Queried} options.
     *
     * @return           a {@link QueryResult} instance.
     *
     * @throws  InvalidStreamsStateException if streams is not running
     */
    public QueryResult<K, V> query(final KafkaStreamsContainer streams, final Queried options) {
        Objects.requireNonNull(streams, "streams cannot be null");
        Objects.requireNonNull(options, "options cannot be null");

        long now = Time.SYSTEM.milliseconds();

        // Quickly check if streams instance is still running
        if (streams.isNotRunning()) {
            throw new InvalidStreamsStateException(
                "streams instance for id '" + streams.applicationId() +
                        "' is not running (" + streams.state().value() + ")"
            );
        }

        QueryResult<K, V> result;
        if (query.isKeyedQuery()) {
            result = querySingleHostStateStore(streams, options);
        } else {
            result = queryMultiHostStateStore(streams, options);
        }
        return result.took(Time.SYSTEM.milliseconds() - now);
    }

    private QueryResult<K, V> queryMultiHostStateStore(final KafkaStreamsContainer streams,
                                                       final Queried options) {

        final List<Either<SuccessResultSet<K, V>, ErrorResultSet>> results = new LinkedList<>();

        Collection<StreamsServerInfo> servers = streams.getAllMetadataForStore(query.storeName());
        if (servers.isEmpty()) {
            String error = "no metadata available for store '" + query.storeName() + "'";
            LOG.warn(error);
            return buildNotAvailableResult(streams.applicationServer(), error);
        }

        List<CompletableFuture<QueryResult<K, V>>> remotes = null;
        if (options.remoteAccessAllowed()) {
            // Forward query to all remote instances

            RemoteQueryContext context = new RemoteQueryContext(streams.applicationServer(), options);
            remotes = servers.stream()
                .filter(Predicate.not(StreamsServerInfo::isLocal))
                .map(context::executeAsyncQueryRemotely)
                .collect(Collectors.toList());
        }
        //Execute the query locally only if the local instance own the queried store.
        LocalQueryContext localQueryContext = new LocalQueryContext(streams);
        servers.stream()
            .filter(StreamsServerInfo::isLocal)
            .findFirst()
            .map(target -> localQueryContext.execute(target, false).getResult().unwrap().get(0)
            ).ifPresent(results::add);

        if (remotes != null) {
            // Blocking
            results.addAll(waitRemoteThenGet(remotes));
        }

        return buildQueryResult(streams.applicationServer(), results);
    }

    private QueryResult<K, V> querySingleHostStateStore(final KafkaStreamsContainer streams,
                                                        final Queried options) {
        final Serializer<K> keySerializer;
        if (query.keySerializer() != null) {
            keySerializer = query.keySerializer();
        } else {
            // Let's try to get the default configured key serializer, fallback to StringSerializer otherwise.
            @SuppressWarnings("unchecked")
            final Serde<K> serde = (Serde<K>) streams.getDefaultKeySerde().orElse(Serdes.String());
            keySerializer = serde.serializer();
        }

        // Try to query
        final Retry retry = Retry
            .withMaxAttempts(options.retries())
            .withFixedWaitDuration(options.retryBackoff())
            .stopAfterDuration(options.queryTimeout())
            .ifExceptionOfType(AzkarraRetriableException.class);

        final String server = streams.applicationServer();
        return Try
            .retriable(() -> querySingleHostStateStore(streams, keySerializer, options), retry)
            .recover(t -> {
                String cause = t.getCause() != null ? t.getCause().getMessage() : t.getMessage();
                String error = "Retries exhausted for querying state store " + query.storeName() + ". " + cause;
                return Try.success(buildNotAvailableResult(server, error).timeout(true));
            }).get();
    }

    /**
     * Execute this key-query either locally or remotely.
     *
     * @param streams       the {@link KafkaStreamsContainer} instance.
     * @param keySerializer the {@link Serializer} used for serializing the key.
     * @param options       the {@link Queried} option.
     *
     * @return              the {@link QueryResult}
     * @throws AzkarraException
     *             A local query can fail if the streams is re-initializing (task migration)
     *             or the store is not initialized (or closed).
     *
     *             A remote query can fail if the remote instance is down and re-balancing has not occurred yet.
     */
    private QueryResult<K, V> querySingleHostStateStore(final KafkaStreamsContainer streams,
                                                        final Serializer<K> keySerializer ,
                                                        final Queried options) throws AzkarraException {
        final String serverName = streams.applicationServer();

        final Optional<StreamsServerInfo> info = streams.getMetadataForStoreAndKey(
            query.storeName(),
            query.key(),
            keySerializer
        );

        if (info.isEmpty()) {
            String error = "no metadata available for store '" + query.storeName() + "', key '" + query.key() + "'";
            return buildNotAvailableResult(serverName, error);
        }

        final StreamsServerInfo targetServer = info.get();

        final QueryContext<K, V> context;
        if (targetServer.isLocal()) {
            context = new LocalQueryContext(streams);
        } else if (options.remoteAccessAllowed()) {
            context = new RemoteQueryContext(streams.applicationServer(), options);
        } else {
            context = (target, failable) -> buildQueryResult(serverName, Collections.emptyList());
        }
        return context.execute(targetServer, true);
    }

    private QueryResult<K, V> buildNotAvailableResult(final String server,
                                                      final String error) {
        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
            .setServer(server)
            .setStatus(QueryStatus.NOT_AVAILABLE)
            .setError(error)
            .build();
    }

    private QueryResult<K, V> buildQueryResult(final String localServerName,
                                               final List<Either<SuccessResultSet<K, V>, ErrorResultSet>> results) {
        final List<ErrorResultSet> errors = results.stream()
                .filter(Either::isRight)
                .map(e -> e.right().get()).collect(Collectors.toList());

        final List<SuccessResultSet<K, V>> success = results.stream()
                .filter(Either::isLeft)
                .map(e -> e.left().get()).collect(Collectors.toList());

        final QueryStatus status = computeStatus(errors, success);

        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
            .setServer(localServerName)
            .setStatus(status)
            .setFailedResultSet(errors)
            .setSuccessResultSet(success)
            .build();
    }

    private QueryResult<K, V> buildInternalErrorResult(final String localServerName,
                                                       final String remoteServerName,
                                                       final Throwable t) {
        final QueryError error = QueryError.of(t);
        final ErrorResultSet errorResultSet = new ErrorResultSet(
            remoteServerName,
            true,
            error);

        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
            .setServer(localServerName)
            .setStatus(QueryStatus.ERROR)
            .setFailedResultSet(errorResultSet)
            .build();
    }

    private QueryStatus computeStatus(final List<ErrorResultSet> errors,
                                      final List<SuccessResultSet<K, V>> success) {
        QueryStatus status;
        if ( !errors.isEmpty() && !success.isEmpty()) {
            status = QueryStatus.PARTIAL;
        } else if (!errors.isEmpty()) {
            status = QueryStatus.ERROR;
        } else if (!success.isEmpty()) {
            status = QueryStatus.SUCCESS;
        } else {
            status = QueryStatus.NO_RESULT;
        }
        return status;
    }

    private static <K, V> List<Either<SuccessResultSet<K, V>, ErrorResultSet>> waitRemoteThenGet(
            final List<CompletableFuture<QueryResult<K, V>>> futures
    )  {
        final CompletableFuture<List<QueryResult<K, V>>> future = futures
                .stream()
                .collect(FutureCollectors.allOf());
        try {
            // TODO must handle exception case.
            return future.handle((results, throwable) ->
                results.stream()
                .map(QueryResult::getResult)
                .flatMap(o -> o.unwrap().stream())
                .collect(Collectors.toList())).get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unexpected error happens while waiting for remote query results", e);
            return Collections.emptyList();
        }
    }



    private interface QueryContext<K, V>  {

        default QueryResult<K, V> execute(final StreamsServerInfo target) throws AzkarraException {
            return execute(target, false);
        }

        QueryResult<K, V> execute(final StreamsServerInfo target, final boolean failable) throws AzkarraException;
    }

    private class LocalQueryContext implements QueryContext<K, V> {

        private final KafkaStreamsContainer streams;

        LocalQueryContext(final KafkaStreamsContainer streams) {
            this.streams = streams;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryResult<K, V> execute(final StreamsServerInfo target, final boolean failable) {
            Try<List<KV<K, V>>> executed = query.execute(streams);

            if (failable && executed.isFailure()) {
                Throwable exception = executed.getThrowable();
                if (exception instanceof InvalidStateStoreException) {
                    throw new AzkarraRetriableException(exception);
                }
                // cannot be retriable, ignored exception.
            }

            final Try<Either<List<KV<K, V>>, List<Error>>> attempt = executed
                .transform(
                    v -> Try.success(Either.left(v)),
                    t -> Try.success(Either.right(Collections.singletonList(new Error(t))))
                );

            final String serverName = streams.applicationServer();
            final Either<SuccessResultSet<K, V>, ErrorResultSet> rs = attempt.get()
                .left()
                .map(records -> new SuccessResultSet<>(serverName, false, records))
                .right()
                .map(errors -> new ErrorResultSet(serverName, false, QueryError.allOf(errors)));

            return buildQueryResult(streams.applicationServer(), Collections.singletonList(rs));
        }
    }

    private class RemoteQueryContext implements QueryContext<K, V>  {

        private final Queried options;
        private final String localServerName;

        RemoteQueryContext(final String localServerName,
                           final Queried options) {
            this.localServerName = localServerName;
            this.options = options;
        }

        private CompletableFuture<QueryResult<K, V>> executeAsyncQueryRemotely(final StreamsServerInfo remote) {
            return executeAsyncQueryRemotely(remote, false);
        }

        private CompletableFuture<QueryResult<K, V>> executeAsyncQueryRemotely(final StreamsServerInfo remote,
                                                                               final boolean failable) {
            CompletableFuture<QueryResult<K, V>> future = remoteQueryClient.query(remote, query, options);
            if (!failable) {
                future.exceptionally(t -> buildInternalErrorResult(localServerName, remote.hostAndPort(), t));
            }
            return future.thenApply(rs -> rs.server(localServerName));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public QueryResult<K, V> execute(final StreamsServerInfo remote, final boolean failable) {
            final String remoteServerName = remote.hostAndPort();
            QueryResult<K, V> result;
            try {
                result = executeAsyncQueryRemotely(remote, failable).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = buildInternalErrorResult(localServerName, remoteServerName, e);
            } catch (ExecutionException e) {
                // cast should be OK.
                LOG.error("Cannot query remote state store. {}", e.getCause().getMessage());
                throw (RuntimeException)e.getCause();
            }
            return result;
        }
    }
}