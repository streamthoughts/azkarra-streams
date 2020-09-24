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

import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Objects;

public abstract class KeyedLocalStoreQuery<T, K, V> implements LocalStoreQuery<K, V> {

    private final String storeName;
    private final T key;
    private final Serializer<T> keySerializer;


    /**
     * Creates a new {@link KeyedLocalStoreQuery} instance.
     *
     * @param storeName     the storeName name.
     * @param key           the record key.
     * @param keySerializer the key serializer.
     */
    KeyedLocalStoreQuery(final String storeName,
                         final T key,
                         final Serializer<T> keySerializer) {
        this.key = key;
        this.storeName = storeName;
        this.keySerializer = keySerializer;
    }

    public T key() {
        return key;
    }

    public String storeName() {
        return storeName;
    }

    public Serializer<T> keySerializer() {
        return keySerializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyedLocalStoreQuery)) return false;
        KeyedLocalStoreQuery<?, ?, ?> that = (KeyedLocalStoreQuery<?, ?, ?>) o;
        return Objects.equals(storeName, that.storeName) &&
               Objects.equals(key, that.key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(storeName, key);
    }
}
