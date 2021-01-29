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

import io.streamthoughts.azkarra.api.errors.AzkarraRetriableException;
import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Either;
import io.streamthoughts.azkarra.api.monad.Retry;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.result.ErrorResultSet;
import io.streamthoughts.azkarra.api.query.result.QueryError;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.query.result.SuccessResultSet;
import io.streamthoughts.azkarra.runtime.streams.LocalKafkaStreamsContainer;
import org.apache.kafka.streams.errors.InvalidStateStoreException;

import java.util.Collections;
import java.util.List;

import static io.streamthoughts.azkarra.runtime.query.internal.QueryResultUtils.buildNotAvailableResult;
import static io.streamthoughts.azkarra.runtime.query.internal.QueryResultUtils.buildQueryResult;

public class LocalQueryCall<K, V> extends BaseAsyncQueryCall<K, V, LocalExecutableQuery<K, V>> {

    private final LocalKafkaStreamsContainer container;

    /**
     * Creates a new {@link DecorateQuery} instance.
     *
     * @param query the query.
     */
    public LocalQueryCall(final LocalKafkaStreamsContainer container,
                          final LocalExecutableQuery<K, V> query) {
        super(query);
        this.container = container;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<K, V> execute(final QueryOptions options) {
        return Try
            .retriable(() -> doExecute(options), toRetry(options))
            .recover(t -> {
                String cause = t.getCause() != null ? t.getCause().getMessage() : t.getMessage();
                String error = "Retries exhausted for querying state store " + query.getStoreName() + ". " + cause;
                final QueryResult<K, V> result = buildNotAvailableResult(getLocalEndpoint(), error);
                return Try.success(result.timeout(true));
            }).get();
    }

    public String getLocalEndpoint() {
        return container.endpoint().get().listener();
    }

    private Retry toRetry(final QueryOptions options) {
        return Retry
            .withMaxAttempts(options.retries())
            .withFixedWaitDuration(options.retryBackoff())
            .stopAfterDuration(options.queryTimeout())
            .ifExceptionOfType(AzkarraRetriableException.class);
    }

    private QueryResult<K, V> doExecute(final QueryOptions options) {
        Try<List<KV<K, V>>> executed = query.execute(container, options.limit());
        if (options.retries() > 0 && executed.isFailure()) {
            Throwable exception = executed.getThrowable();
            if (exception instanceof InvalidStateStoreException) {
                throw new AzkarraRetriableException(exception);
            }
            // Can't be retriable, ignored exception.
        }

        final Try<Either<List<KV<K, V>>, List<Error>>> attempt = executed
            .transform(
                v -> Try.success(Either.left(v)),
                t -> Try.success(Either.right(Collections.singletonList(new Error(t))))
            );

        final Either<SuccessResultSet<K, V>, ErrorResultSet> rs = attempt.get()
            .left()
            .map(records -> new SuccessResultSet<>(getLocalEndpoint(), false, records))
            .right()
            .map(errors -> new ErrorResultSet(getLocalEndpoint(), false, QueryError.allOf(errors)));

        return buildQueryResult(getLocalEndpoint(), Collections.singletonList(rs));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryCall<K, V> renew() {
        return new LocalQueryCall<>(container, query);
    }


}
