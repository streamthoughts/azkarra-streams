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

import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.result.QueryResult;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsContainer;

/**
 * A {@code QueryExecutionDelegatee} is used to plug a specific strategy to execute a state store query.
 *
 * @see DefaultInteractiveQueryService
 */
public interface QueryExecutionDelegatee {

    /**
     * Gets the {@link KafkaStreamsContainer} class that is supported by this {@link QueryExecutionDelegatee}.
     *
     * @return      the supported {@link KafkaStreamsContainer} class.
     */
    Class<? extends KafkaStreamsContainer> supportedClass();

    /**
     * Executed the provided {@link QueryRequest} with the specified {@link QueryOptions} and the
     * {@link KafkaStreamsContainer}.
     *
     * @param container     the {@link KafkaStreamsContainer} object.
     * @param queryRequest  the {@link QueryRequest} to be execute.
     * @param queryOptions  the {@link QueryOptions} that should be used for executing the query.
     * @param <K>           the expected type for the record-key.
     * @param <V>           the expected type for the record-value.
     * @return              the {@link QueryResult}.
     */
    <K, V> QueryResult<K, V> execute(final KafkaStreamsContainer container,
                                     final QueryRequest queryRequest,
                                     final QueryOptions queryOptions);
}