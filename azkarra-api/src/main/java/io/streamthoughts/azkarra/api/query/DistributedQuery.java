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

import io.streamthoughts.azkarra.api.monad.Either;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import io.streamthoughts.azkarra.api.streams.StreamsServerInfo;
import io.streamthoughts.azkarra.api.query.internal.PreparedQuery;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.GlobalResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.QueryResultBuilder;
import io.streamthoughts.azkarra.api.query.result.QueryStatus;
import io.streamthoughts.azkarra.api.time.Time;
import io.streamthoughts.azkarra.api.util.FutureCollectors;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
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
 * Default class to query a state storeName either locally or remotely.
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
     * Executes a state storeName query for specified arguments.
     *
     * @param streams    the {@link KafkaStreamsContainer} instance on which to execute this query.
     * @param options    the {@link Queried} options.
     *
     * @return           a {@link QueryResult} instance.
     */
    public QueryResult<K, V> query(final KafkaStreamsContainer streams,
                                   final Queried options) {
        Objects.requireNonNull(streams, "streams cannot be null");
        Objects.requireNonNull(options, "options cannot be null");

        long now = Time.SYSTEM.milliseconds();

        Optional<StreamsServerInfo> localServerInfo = streams.getLocalServerInfo();

        final String localServerName = localServerInfo.map(StreamsServerInfo::hostAndPort).orElse("N/A");

        QueryResult<K, V> result;
        if (query.isKeyedQuery()) {
            result = querySingleHostStateStore(streams, localServerName, options, now);
        } else {
            result = queryMultiHostStateStore(streams, localServerName, options, now);
        }
        return result;
    }

    private QueryResult<K, V> queryMultiHostStateStore(final KafkaStreamsContainer streams,
                                                       final String localServerName,
                                                       final Queried options,
                                                       final long now) {
        List<CompletableFuture<QueryResult<K, V>>> remotes = null;

        final List<Either<SuccessResultSet<K, V>, ErrorResultSet>> results = new LinkedList<>();

        Collection<StreamsServerInfo> servers = streams.getAllMetadataForStore(query.storeName());

        if (servers.isEmpty()) {
            return buildStoreNotFoundResult(localServerName, now);
        }

        if (options.remoteAccessAllowed()) {
            // Forward query to all remote instances
            remotes = servers.stream()
                .filter(Predicate.not(StreamsServerInfo::isLocal))
                .map(server -> {
                    final String remoteServerName = server.hostAndPort();
                    CompletableFuture<QueryResult<K, V>> query = remoteQueryClient.query(
                        server,
                        this.query,
                        options.withRemoteAccessAllowed(false)
                    );
                    return query.exceptionally(t ->
                        buildInternalErrorResult(localServerName, remoteServerName, t, now));
                })
                .collect(Collectors.toList());
        }
        //Execute the query locally only if the local instance own the queried store.
        servers.stream()
            .filter(StreamsServerInfo::isLocal)
            .findFirst()
            .ifPresent(o -> results.add(executeLocallyAndGet(localServerName, streams, options)));

        if (remotes != null) {
            // Blocking
            results.addAll(waitRemoteThenGet(remotes));
        }

        return buildQueryResult(localServerName, results, now);
    }

    @SuppressWarnings("unchecked")
    private QueryResult<K, V> querySingleHostStateStore(final KafkaStreamsContainer streams,
                                                        final String localServerName,
                                                        final Queried options,
                                                        final long now) {

        QueryResult<K, V> result = null;

        Serializer<K> keySerializer = query.keySerializer();
        if (keySerializer == null) {
            // Let's try to get the default configured key serializer, fallback to StringSerializer otherwise.
            final Serde<K> serde = (Serde<K>) streams.getDefaultKeySerde().orElse(Serdes.String());
            keySerializer = serde.serializer();
        }

        final StreamsServerInfo serverInfoForKey = streams.getMetadataForStoreAndKey(
            query.storeName(),
            query.key(),
            keySerializer
        );

        if (serverInfoForKey == null) {
            return buildStoreNotFoundResult(localServerName, now);
        }

        if (serverInfoForKey.isLocal()) {
            // Execute the query locally
            final Either<SuccessResultSet<K, V>, ErrorResultSet> localResultSet =
                executeLocallyAndGet(localServerName, streams, options);
            result = buildQueryResult(localServerName, Collections.singletonList(localResultSet), now);

        } else if (options.remoteAccessAllowed()) {

            final String remoteServerName = serverInfoForKey.hostAndPort();
            try {
                CompletableFuture<QueryResult<K, V>> future = remoteQueryClient.query(serverInfoForKey, query, options);
                result =  future
                    .exceptionally(t -> buildInternalErrorResult(localServerName, remoteServerName, t, now))
                    .get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = buildInternalErrorResult(localServerName, remoteServerName, e, now);
            } catch (ExecutionException e) {
                /* cannot happens */
            }
        }

        if (result == null) {
            // build empty result.
            result = buildQueryResult(localServerName, Collections.emptyList(), now);
        }

        return result;
    }

    private QueryResult<K, V> buildStoreNotFoundResult(final String server, final long now) {
        ErrorResultSet error = new ErrorResultSet(
            server,
            false,
            new QueryError("no metadata found for store '" + query.storeName() + "'")
        );
        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
            .setServer(server)
            .setTook(Time.SYSTEM.milliseconds() - now)
            .setStatus(QueryStatus.NOT_AVAILABLE)
            .setFailedResultSet(Collections.singletonList(error))
            .build();
    }

    private QueryResult<K, V> buildQueryResult(final String localServerName,
                                               final List<Either<SuccessResultSet<K, V>, ErrorResultSet>> results,
                                               final long now) {
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
            .setTook(Time.SYSTEM.milliseconds() - now)
            .setStatus(status)
            .setFailedResultSet(errors)
            .setSuccessResultSet(success)
            .build();
    }

    private QueryResult<K, V> buildInternalErrorResult(final String localServerName,
                                                       final String remoteServerName,
                                                       final Throwable t,
                                                       final long now) {
        final QueryError error = QueryError.of(t);
        final ErrorResultSet errorResultSet = new ErrorResultSet(
            remoteServerName,
            true,
            error);

        final QueryResultBuilder<K, V> builder = QueryResultBuilder.newBuilder();
        return builder
            .setServer(localServerName)
            .setTook(Time.SYSTEM.milliseconds() - now)
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
                .map(result -> (GlobalResultSet<K, V>)result.getResult())
                .flatMap(o -> o.unwrap().stream())
                .collect(Collectors.toList())).get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unexpected error happens while waiting for remote query results", e);
            return Collections.emptyList();
        }
    }

    private Either<SuccessResultSet<K, V>, ErrorResultSet> executeLocallyAndGet(final String serverName,
                                                                                final KafkaStreamsContainer streams,
                                                                                final Queried options) {

        return query.execute(streams, options)
             .left()
             .map(records -> new SuccessResultSet<>(serverName, false, records))
             .right()
             .map(errors -> new ErrorResultSet(serverName, false, QueryError.allOf(errors)));
    }
}