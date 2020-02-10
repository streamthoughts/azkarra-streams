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
import io.streamthoughts.azkarra.api.monad.Try;
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.QueryInfo;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PreparedQuery<K, V> extends QueryInfo {

    private final Logger LOG = LoggerFactory.getLogger(PreparedQuery.class);

    private final QueryParams parameters;

    private final String  storeName;

    private final LocalStoreQuery<K, V> localStoreQuery;

    PreparedQuery(final QueryParams parameters,
                  final String storeName,
                  final LocalStoreQuery<K, V> localStoreQuery) {
        super(storeName, localStoreQuery.storeType(), localStoreQuery.operationType(), parameters);
        this.parameters = parameters;
        this.storeName = storeName;
        this.localStoreQuery = localStoreQuery;
    }

    public boolean isKeyedQuery() {
        return localStoreQuery instanceof KeyedLocalStoreQuery;
    }

    public K key() {
        return ((KeyedLocalStoreQuery<K, ?, V>)localStoreQuery).key();
    }

    public Serializer<K> keySerializer() {
        return ((KeyedLocalStoreQuery<K, ?, V>)localStoreQuery).keySerializer();
    }

    /**
     * Executes this query locally on the specified streams instance.
     *
     * @param container the {@link KafkaStreamsContainer} instance.
     */
    public Try<List<KV<K, V>>> execute(final KafkaStreamsContainer container, final long limit) {
        return logFailure(Try.success(localStoreQuery).flatMap(q -> q.execute(container, limit)));
    }

    private Try<List<KV<K, V>>> logFailure(final Try<List<KV<K, V>>> executed) {
        if (executed.isFailure()) {
            LOG.error("Error happens while executing query '{}' on state store '{}' with params '{}': {}",
                localStoreQuery.operationType(),
                storeName,
                parameters,
                executed.getThrowable().getMessage());
        }
        return executed;
    }
}
