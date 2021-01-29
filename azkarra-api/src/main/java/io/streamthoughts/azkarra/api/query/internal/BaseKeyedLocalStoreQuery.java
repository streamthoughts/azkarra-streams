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

import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.LocalExecutableQueryWithKey;
import io.streamthoughts.azkarra.api.query.Query;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Objects;

public abstract class BaseKeyedLocalStoreQuery<K1, K2, V>
        extends DecorateQuery<Query>
        implements LocalExecutableQueryWithKey<K1, K2, V> {

    private final K1 key;
    private final Serializer<K1> keySerializer;

    /**
     * Creates a new {@link BaseKeyedLocalStoreQuery} instance.
     *
     * @param query         the {@link Query}.
     * @param key           the record key.
     * @param keySerializer the key serializer.
     */
    BaseKeyedLocalStoreQuery(final Query query,
                             final K1 key,
                             final Serializer<K1> keySerializer) {
        super(query);
        this.key = Objects.requireNonNull(key, "key should not be null");
        this.keySerializer = Objects.requireNonNull(keySerializer, "keySerializer should not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public K1 getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializer<K1> getKeySerializer() {
        return keySerializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseKeyedLocalStoreQuery)) return false;
        if (!super.equals(o)) return false;
        BaseKeyedLocalStoreQuery<?, ?, ?> that = (BaseKeyedLocalStoreQuery<?, ?, ?>) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(keySerializer, that.keySerializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), key, keySerializer);
    }

}
