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
import io.streamthoughts.azkarra.api.monad.Reader;
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlySessionStore;

import java.util.List;

public class SessionFetchKeyRangeQuery<K, V> implements LocalStoreQuery<Windowed<K>, V> {

    private final K keyFrom;

    private final K keyTo;

    private final String store;

    /**
     * Creates a new {@link SessionFetchKeyRangeQuery} instance.
     *
     * @param store     the store name.
     * @param keyFrom   the query param key from.
     * @param keyTo     the query param key to.
     */
    SessionFetchKeyRangeQuery(final String store,
                              final K keyFrom,
                              final K keyTo) {
        this.keyFrom = keyFrom;
        this.keyTo = keyTo;
        this.store = store;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreType storeType() {
        return StoreType.SESSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreOperation operationType() {
        return StoreOperation.FETCH_KEY_RANGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<Windowed<K>, V>>>  execute(final KafkaStreamsContainer container) {

        final LocalStoreAccessor<ReadOnlySessionStore<K, V>> accessor = container.getLocalSessionStore(store);

        final Reader<ReadOnlySessionStore<K, V>, List<KV<Windowed<K>, V>>> reader =
            reader(keyFrom, keyTo)
           .map(LocalStoreQuery::toKeyValueListAndClose);

        return new LocalStoreQueryExecutor<>(accessor).execute(reader);
    }

    private Reader<ReadOnlySessionStore<K, V>, KeyValueIterator<Windowed<K>, V>> reader(final K keyFrom,
                                                                                        final K keyTo) {
        return Reader.of(store -> store.fetch(keyFrom, keyTo));
    }
}
