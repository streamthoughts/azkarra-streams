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

import io.streamthoughts.azkarra.api.query.error.InvalidQueryException;
import io.streamthoughts.azkarra.api.query.result.QueryResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A {@code QueryCall} is a query that has been prepared for execution.
 *
 * @param <K>   type of the record-key.
 * @param <V>   type of the record-value.
 */
public interface QueryCall<K, V> extends Query {

    /**
     * Executes this {@link QueryCall}.
     *
     * @param options   the options to be used.
     *
     * @return          the {@link QueryResult}.
     * @throws IllegalStateException when the {@code QueryCall} has already been executed.
     */
    QueryResult<K, V> execute(final QueryOptions options);

    /**
     * Executes this {@link QueryCall} asynchronously.
     *
     * @param options   the options to be used.
     *
     * @return          a {@link CompletableFuture} of {@link QueryResult}.
     * @throws IllegalStateException when the {@code QueryCall} has already been executed.
     */
    CompletableFuture<QueryResult<K, V>> executeAsync(final QueryOptions options);

    /**
     * Executes this {@link QueryCall} asynchronously.
     *
     * @param options           the options to be used.
     * @param resultCallback    the callback to be invoked when this {@code QueryCall} complete
     *                          with either a success result or a failure exception.
     *
     * @throws IllegalStateException when the {@code QueryCall} has already been executed.
     */
    void executeAsync(final QueryOptions options, final Consumer<QueryResult<K, V>> resultCallback);

    /**
     * Cancels the {@link QueryCall}, if possible. {@link QueryCall} that are already complete cannot be canceled.
     */
    void cancel();

    /**
     * @return {@code true} if this {@link QueryCall} has already been executed.
     */
    boolean isExecuted();


    /**
     * @return {@code true} if this {@link QueryCall} has already been canceled.
     */
    boolean isCanceled();

    /**
     * Create a new, identical {@code QueryCall} to this one which can be  re-executed even if this call
     * has already been.
     */
    QueryCall<K, V> renew();

    /**
     * A {@code QueryCallFactory} is used to create new {@link QueryCall}.
     */
    interface QueryCallFactory {

        /**
         * Creates a new {@link QueryCall} for specified {@link QueryRequest}.
         *
         * @param request   the query.
         * @param <K>       the expected type for record-key.
         * @param <V>       the expected type for record-value.
         * @return          a new {@link QueryCall} object.
         *
         * @throws InvalidQueryException    if the given request is invalid.
         */
        <K, V> QueryCall<K, V> newQueryCall(final QueryRequest request) throws InvalidQueryException;
    }
}
