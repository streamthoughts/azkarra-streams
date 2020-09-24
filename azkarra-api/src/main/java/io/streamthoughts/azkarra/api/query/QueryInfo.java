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

import java.util.Objects;

/**
 * Class which is use to wrap information about the store to be query.
 */
public class QueryInfo {

    private final String storeName;
    private final StoreOperation storeOperation;
    private final StoreType storeType;
    private final QueryParams parameters;

    /**
     * Creates a new {@link QueryInfo} instance.
     *
     * @param storeName         the store name.
     * @param storeType         the store type.
     * @param storeOperation    the store operation.
     * @param parameters        the query parameters.
     */
    public QueryInfo(final String storeName,
                     final StoreType storeType,
                     final StoreOperation storeOperation,
                     final QueryParams parameters) {
        this.storeName = storeName;
        this.storeOperation = storeOperation;
        this.storeType = storeType;
        this.parameters = parameters;
    }

    public QueryParams parameters() {
        return parameters;
    }

    public String storeName() {
        return storeName;
    }

    public StoreOperation operation() {
        return storeOperation;
    }

    public StoreType type() {
        return storeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryInfo)) return false;
        QueryInfo that = (QueryInfo) o;
        return Objects.equals(storeName, that.storeName) &&
                storeOperation == that.storeOperation &&
                storeType == that.storeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(storeName, storeOperation, storeType);
    }

    @Override
    public String toString() {
        return "QueryInfo{" +
                "storeName=" + storeName +
                ", storeOperation=" + storeOperation +
                ", storeType=" + storeType +
                ", parameters=" + parameters +
                '}';
    }
}
