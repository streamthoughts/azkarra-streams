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
package io.streamthoughts.azkarra.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kafka.streams.KeyValue;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Simple key/value pair.
 *
 * @param <K>   the key type.
 * @param <V>   the value type.
 */
public class KV<K, V> implements Comparable<KV<K, V>>, Serializable {

    private final K key;
    private final V value;

    public static <K, V> KV<K, V> of(final K key, V value) {
        return new KV<>(key, value);
    }

    public static <K, V> KV<K, V> of(final KeyValue<K, V> kv) {
        return new KV<>(kv.key, kv.value);
    }

    /**
     * Creates a new {@link KV} instance.
     *
     * @param key       the record key.
     * @param value     the record value.
     */
    @JsonCreator
    public KV(@JsonProperty("key") final K key,
              @JsonProperty("value") final V value) {
        this.key = key;
        this.value = value;
    }

    public Optional<K> nullableKey() {
        return Optional.ofNullable(key);
    }

    @JsonProperty("key")
    public K key() {
        return key;
    }

    public Optional<V> nullableValue() {
        return Optional.ofNullable(value);
    }

    @JsonProperty("value")
    public V value() {
        return value;
    }

    public KeyValue<K, V> toKafkaKeyValue() {
        return KeyValue.pair(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KV)) return false;
        KV<?, ?> keyValue = (KV<?, ?>) o;
        return Objects.equals(key, keyValue.key) &&
                Objects.equals(value, keyValue.value);
    }

    @JsonIgnore
    public boolean isNullKey() {
        return key == null;
    }

    public KV<V, K> swap() {
        return new KV<>(value, key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[key=" + key + ", value=" + value + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final KV<K, V> that) {
        if (that.isNullKey()) return -1;
        if (this.isNullKey()) return 1;
        return this.key.toString().compareTo(that.key.toString());
    }
}
