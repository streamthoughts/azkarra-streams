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
package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.query.internal.QueryBuilder;
import io.streamthoughts.azkarra.api.query.internal.QueryOperationBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The store types supported by {@link org.apache.kafka.streams.KafkaStreams}.
 */
public enum StoreType {

    /**
     * Key-Value store.
     */
    KEY_VALUE {
        @Override
        public QueryOperationBuilder getQueryBuilder(final String storeName) {
            return new QueryBuilder(storeName).keyValue();
        }
    },
    /**
     * Window store.
     */
    WINDOW {
        @Override
        public QueryOperationBuilder getQueryBuilder(final String storeName) {
            return new QueryBuilder(storeName).window();
        }
    },
    /**
     * Session store.
     */
    SESSION {
        @Override
        public QueryOperationBuilder getQueryBuilder(final String storeName) {
            return new QueryBuilder(storeName).session();
        }
    },
    /**
     * Timestamped Key-Value store.
     */
    TIMESTAMPED_KEY_VALUE {
        @Override
        public QueryOperationBuilder getQueryBuilder(final String storeName) {
            return new QueryBuilder(storeName).timestampedKeyValue();
        }
    },
    /**
     * Timestamped Window store
     */
    TIMESTAMPED_WINDOW {
        @Override
        public QueryOperationBuilder getQueryBuilder(final String storeName) {
            return new QueryBuilder(storeName).timestampedWindow();
        }
    };

    private static final Map<String, StoreType> CACHE = new HashMap<>();

    static {
        Arrays.stream(StoreType.values())
              .forEach(t -> CACHE.put(t.name(), t));
    }


    public static Optional<StoreType> parse(final String storeType) {
        final String uppercased = storeType.toUpperCase();
        return Optional.ofNullable(CACHE.get(uppercased));
    }

    public abstract QueryOperationBuilder getQueryBuilder(final String storeName);

    public String prettyName() {
        return name().toLowerCase();
    }
}
