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
package io.streamthoughts.azkarra.api.query.internal;

import io.streamthoughts.azkarra.api.model.KV;
import io.streamthoughts.azkarra.api.monad.Retry;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.monad.Reader;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.Queried;
import org.apache.kafka.streams.errors.InvalidStateStoreException;

import java.util.List;
import java.util.function.Function;

/**
 * @param <S> the read-only storeName type.
 */
class LocalStoreQueryExecutor<S> {

    private final LocalStoreAccessor<S> storeAccess;

    /**
     * Creates a new {@link LocalStoreQueryExecutor} instance.
     *
     * @param accessor the {@link LocalStoreAccessor} instance.
     */
    LocalStoreQueryExecutor(final LocalStoreAccessor<S> accessor) {
        this.storeAccess = accessor;
    }

    /**
     * Executes this execute on the specified instance with the specified options.
     *
     * @param reader     the {@link Reader} instance.
     * @param options    the {@link Queried} options.
     * @return           the execute result.
     */
    <K,V> Try<List<KV<K, V>>> execute(final Reader<S, ? extends List<KV<K, V>>> reader,
                                      final Queried options) {

        final AttemptToReadStore<KV<K, V>> attempt = new AttemptToReadStore<>(reader, options);
        return storeAccess.get(options).flatMap(attempt);
    }

    private final class AttemptToReadStore<V> implements Function<S, Try<List<V>>> {

        final Reader<S, ? extends List<V>> reader;
        final Queried options;

        AttemptToReadStore(final Reader<S, ? extends List<V>> reader, final Queried options) {
            this.reader = reader;
            this.options = options;
        }

        @Override
        public Try<List<V>> apply(final S store) {
            return Try.retriable(() -> reader.apply(store), Retry
                .withMaxAttempts(options.retries())
                .withFixedWaitDuration(options.retryBackoff())
                .stopAfterDuration(options.queryTimeout())
                .ifExceptionOfType(InvalidStateStoreException.class));
        }
    }
}
