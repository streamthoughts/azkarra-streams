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
package io.streamthoughts.azkarra.http.query;

import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.internal.QueryConstants;
import io.streamthoughts.azkarra.client.openapi.models.V1Query;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchAllParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchKeyRangeParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryFetchTimeRangeParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryGetParams;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryOperation;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryOptions;
import io.streamthoughts.azkarra.client.openapi.models.V1QueryRangeParams;

import java.util.Collections;

public class V1QueryBuilder {

    /**
     * Helper method to create a new {@link V1Query} object based on
     * the given {@link QueryRequest} and {@link QueryOptions}.
     *
     * @param query   the {@link QueryRequest}.
     * @param options the {@link QueryOptions}.
     * @return a new {@link V1Query}.
     */
    public static V1Query create(final QueryRequest query,
                                 final QueryOptions options) {
        final QueryParams params = query.getParams();
        final V1QueryOperation operation = new V1QueryOperation();
        switch (query.getStoreOperation()) {
            case GET:
                operation.get(new V1QueryGetParams()
                        .key(params.getString(QueryConstants.QUERY_PARAM_KEY))
                );
                break;
            case ALL:
                operation.all(Collections.emptyMap());
                break;
            case FETCH:
                operation.fetch(new V1QueryFetchParams()
                        .key(params.getString(QueryConstants.QUERY_PARAM_KEY))
                        .time(params.getLong(QueryConstants.QUERY_PARAM_TIME))
                );
                break;
            case FETCH_KEY_RANGE:
                operation.fetchKeyRange(new V1QueryFetchKeyRangeParams()
                        .keyFrom(params.getString(QueryConstants.QUERY_PARAM_KEY_FROM))
                        .keyTo(params.getString(QueryConstants.QUERY_PARAM_KEY_TO))
                        .timeFrom(params.getLong(QueryConstants.QUERY_PARAM_TIME_FROM))
                        .timeTo(params.getLong(QueryConstants.QUERY_PARAM_TIME_TO))
                );
                break;
            case FETCH_TIME_RANGE:
                operation.fetchTimeRange(new V1QueryFetchTimeRangeParams()
                        .key(params.getString(QueryConstants.QUERY_PARAM_KEY))
                        .timeFrom(params.getLong(QueryConstants.QUERY_PARAM_TIME_FROM))
                        .timeTo(params.getLong(QueryConstants.QUERY_PARAM_TIME_TO))
                );
                break;
            case FETCH_ALL:
                operation.fetchAll(new V1QueryFetchAllParams()
                        .timeFrom(params.getLong(QueryConstants.QUERY_PARAM_TIME_FROM))
                        .timeTo(params.getLong(QueryConstants.QUERY_PARAM_TIME_TO))
                );
                break;
            case RANGE:
                operation.range(new V1QueryRangeParams()
                        .keyFrom(params.getString(QueryConstants.QUERY_PARAM_KEY_FROM))
                        .keyTo(params.getString(QueryConstants.QUERY_PARAM_KEY_TO))
                );
                break;
            case COUNT:
                operation.count(Collections.emptyMap());
                break;
        }

        return new V1Query()
                .type(V1Query.TypeEnum.fromValue(query.getStoreType().prettyName()))
                .query(operation)
                .setOptions(
                        new V1QueryOptions()
                                .limit(options.limit())
                                .queryTimeoutMs(options.queryTimeout().toMillis())
                                .remoteAccessAllowed(options.remoteAccessAllowed())
                                .retryBackoffMs(options.retryBackoff().toMillis())
                                .retries(options.retries())
                );
    }
}
