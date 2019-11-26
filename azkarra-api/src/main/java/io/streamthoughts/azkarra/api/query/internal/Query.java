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

import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.query.LocalStoreQuery;
import io.streamthoughts.azkarra.api.query.QueryParams;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Query<K, V> {

    private final String storeName;
    private final LocalStoreQueryBuilder<K, V> localStoreQueryBuilder;

    /**
     * Creates a new {@link Query} instance.
     *
     * @param storeName       the storeName name.
     * @param builder         the {@link LocalStoreQueryBuilder} instance.
     */
    Query(final String storeName,
          final LocalStoreQueryBuilder<K, V> builder) {
        Objects.requireNonNull(storeName, "storeName cannot be null");
        Objects.requireNonNull(builder, "builder cannot be null");
        this.storeName = storeName;
        this.localStoreQueryBuilder = builder;
    }

    public Optional<List<Error>> validate(final QueryParams parameters) {
        Objects.requireNonNull(parameters, "parameters cannot be null");
        return localStoreQueryBuilder.validates(parameters)
                .toEither()
                .right()
                .toOptional();
    }

    public PreparedQuery<K, V> prepare() {
        return prepare(QueryParams.empty());
    }

    public PreparedQuery<K, V> prepare(final QueryParams parameters) {
        Objects.requireNonNull(parameters, "parameters cannot be null");
        LocalStoreQuery<K, V> query = localStoreQueryBuilder.build(storeName, parameters);
        return new PreparedQuery<>(parameters, storeName, query);
    }
}
