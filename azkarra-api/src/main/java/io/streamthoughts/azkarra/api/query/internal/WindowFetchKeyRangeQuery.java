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
import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.GenericQueryParams;
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessProvider;
import io.streamthoughts.azkarra.api.query.LocalStoreAccessor;
import io.streamthoughts.azkarra.api.query.Query;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;

import java.time.Instant;
import java.util.List;

import static io.streamthoughts.azkarra.api.util.Utils.capped;

public class WindowFetchKeyRangeQuery<K, V>
        extends DecorateQuery<Query>
        implements LocalExecutableQuery<Windowed<K>, V> {

    private final K keyFrom;
    private final K keyTo;
    private final Instant timeFrom;
    private final Instant timeTo;

    /**
     * Creates a new {@link WindowFetchKeyRangeQuery} instance.
     *
     * @param store         the name of the store.
     * @param fromKey       the query param key from.
     * @param toKey         the query param key to.
     * @param timeFrom      the query param time from.
     * @param timeTo        the query param time to.
     */
    WindowFetchKeyRangeQuery(final String store,
                             final K fromKey,
                             final K toKey,
                             final Instant timeFrom,
                             final Instant timeTo) {
        super(createQuery(store, fromKey,toKey, timeFrom, timeTo));
        this.keyFrom = fromKey;
        this.keyTo = toKey;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
    }

    private static QueryRequest createQuery(final String store,
                                            final Object fromKey,
                                            final Object toKey,
                                            final Instant timeFrom,
                                            final Instant timeTo) {
        return new QueryRequest()
                .storeName(store)
                .storeType(StoreType.WINDOW)
                .storeOperation(StoreOperation.FETCH_KEY_RANGE)
                .params(new GenericQueryParams()
                        .put(QueryConstants.QUERY_PARAM_KEY_FROM, fromKey)
                        .put(QueryConstants.QUERY_PARAM_KEY_TO, toKey)
                        .put(QueryConstants.QUERY_PARAM_TIME_FROM, capped(timeFrom).toEpochMilli())
                        .put(QueryConstants.QUERY_PARAM_TIME_TO, capped(timeTo).toEpochMilli())
                );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Try<List<KV<Windowed<K>, V>>> execute(final LocalStoreAccessProvider provider, final long limit) {

        final LocalStoreAccessor<ReadOnlyWindowStore<K, V>> accessor = provider.localWindowStore(getStoreName());

        final Reader<ReadOnlyWindowStore<K, V>, List<KV<Windowed<K>, V>>> reader =
            reader(keyFrom, keyTo, timeFrom, timeTo)
            .map(iterator -> LocalExecutableQuery.toKeyValueListAndClose(iterator, limit));

        return new LocalStoreQueryExecutor<>(accessor).execute(reader);
    }

    private Reader<ReadOnlyWindowStore<K, V>, KeyValueIterator<Windowed<K>, V>> reader(final K keyFrom,
                                                                                       final K keyTo,
                                                                                       final Instant timeFrom,
                                                                                       final Instant timeTo) {
        return Reader.of(store -> store.fetch(keyFrom, keyTo, timeFrom, timeTo));
    }
}
