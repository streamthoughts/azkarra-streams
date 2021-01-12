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

package io.streamthoughts.azkarra.api.query;

import io.streamthoughts.azkarra.api.errors.InvalidStreamsStateException;
import io.streamthoughts.azkarra.api.query.internal.Query;
import io.streamthoughts.azkarra.api.query.result.QueryResult;

public interface InteractiveQueryService {

    /**
     * Executes an interactive query for the specified streams application.
     *
     * @param applicationId the streams application id.
     * @param query         the {@link Query} instance.
     * @param parameters    the query parameters.
     * @param options       the query execution options.
     *
     * @return              the {@link QueryResult} instance.
     *
     * @throws InvalidStreamsStateException if the streams is not running for the given applicationId.
     */
    <K, V> QueryResult<K, V>  query(final String applicationId,
                                    final Query<K, V> query,
                                    final QueryParams parameters,
                                    final Queried options);
}
