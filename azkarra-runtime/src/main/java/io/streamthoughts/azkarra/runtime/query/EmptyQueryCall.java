/*
 * Copyright 2019-2021 StreamThoughts.
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
package io.streamthoughts.azkarra.runtime.query;

import io.streamthoughts.azkarra.api.query.DecorateQuery;
import io.streamthoughts.azkarra.api.query.Query;
import io.streamthoughts.azkarra.api.query.QueryCall;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.result.QueryResult;

import java.util.Collections;

import static io.streamthoughts.azkarra.runtime.query.internal.QueryResultUtils.buildQueryResult;

public class EmptyQueryCall<K, V> extends BaseAsyncQueryCall<K, V, Query> {

    private final String localServerName;

    /**
     * Creates a new {@link DecorateQuery} instance.
     *
     * @param query the query.
     */
    public EmptyQueryCall(final String localServerName,
                          final Query query) {
        super(query);
        this.localServerName = localServerName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryResult<K, V> execute(final QueryOptions options) {
        return buildQueryResult(localServerName, Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryCall<K, V> renew() {
        return new EmptyQueryCall<>(localServerName, query);
    }
}
