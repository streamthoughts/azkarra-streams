/*
 * Copyright 2019-2020 StreamThoughts.
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
import io.streamthoughts.azkarra.api.monad.Reader;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;

import java.util.List;
import java.util.function.Function;

/**
 * @param <S> the read-only store type.
 */
class LocalStoreQueryExecutor<S> {

    private final LocalStoreAccessor<S> store;

    /**
     * Creates a new {@link LocalStoreQueryExecutor} instance.
     *
     * @param accessor the {@link LocalStoreAccessor} instance.
     */
    LocalStoreQueryExecutor(final LocalStoreAccessor<S> accessor) {
        this.store = accessor;
    }

    /**
     * Executes this execute on the specified instance with the specified options.
     *
     * @param reader     the {@link Reader} instance.
     * @return           the execute result.
     */
    <K,V> Try<List<KV<K, V>>> execute(final Reader<S, ? extends List<KV<K, V>>> reader) {
        return store.get().flatMap(new AttemptToReadStore<>(reader));
    }

    private final class AttemptToReadStore<V> implements Function<S, Try<List<V>>> {

        final Reader<S, ? extends List<V>> reader;

        AttemptToReadStore(final Reader<S, ? extends List<V>> reader) {
            this.reader = reader;
        }

        @Override
        public Try<List<V>> apply(final S store) {
            return Try.failable(() -> reader.apply(store));
        }
    }
}
