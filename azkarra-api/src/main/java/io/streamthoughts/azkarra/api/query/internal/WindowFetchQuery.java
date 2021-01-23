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
import io.streamthoughts.azkarra.api.query.GenericQueryParams;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessProvider;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class WindowFetchQuery<K, V> extends BaseKeyedLocalStoreQuery<K, K, V> {

    private final long time;

    /**
     * Creates a new {@link WindowFetchQuery} instance.
     *
     * @param store         the name of the store.
     * @param key           the record key.
     * @param keySerializer the key serializer.
     */
    WindowFetchQuery(final String store,
                     final K key,
                     final Serializer<K> keySerializer,
                     final long time) {
        super(createQuery(store, key, time), key, keySerializer);
        this.time = time;
    }

    private static QueryRequest createQuery(final String store,
                                            final Object key,
                                            final long time) {
        return new QueryRequest()
                .storeName(store)
                .storeType(StoreType.WINDOW)
                .storeOperation(StoreOperation.FETCH_TIME_RANGE)
                .params(new GenericQueryParams()
                        .put(QueryConstants.QUERY_PARAM_KEY, key)
                        .put(QueryConstants.QUERY_PARAM_TIME, time)
                );

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<K, V>>> execute(final LocalStoreAccessProvider provider, final long limit) {

        final LocalStoreAccessor<ReadOnlyWindowStore<K, V>> accessor = provider.localWindowStore(getStoreName());

        final Reader<ReadOnlyWindowStore<K, V>, List<KV<K, V>>> reader =
            reader(getKey(), time).map(value -> Optional.ofNullable(value)
                .map(v -> Collections.singletonList(new KV<>(getKey(), v)))
                .orElse(Collections.emptyList()));

        return new LocalStoreQueryExecutor<>(accessor).execute(reader);
    }

    private Reader<ReadOnlyWindowStore<K, V>, V> reader(final K key, final long time) {
        return Reader.of(store -> store.fetch(key, time));
    }
}
