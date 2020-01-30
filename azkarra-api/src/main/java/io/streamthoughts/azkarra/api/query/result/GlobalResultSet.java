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
package io.streamthoughts.azkarra.api.query.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.monad.Either;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GlobalResultSet<K, V> implements Serializable {

    /**
     * The name of the store name being queried.
     */
    private final String store;

    /**
     * The type of the store type being queried.
     */
    private final String type;

    /**
     * The total number of records returned.
     */
    private final Long total;

    /**
     * The global query result.
     */
    private String error;

    /**
     * The returned records.
     */
    private List<ErrorResultSet> failure;

    /**
     * The returned records.
     */
    private List<SuccessResultSet<K, V>> success;

    /**
     * Creates a new {@link GlobalResultSet} instance.
     *
     * @param store   the name of the store.
     * @param type    the type of the store.
     * @param failure the error record set.
     * @param success the result record set.
     */
    public GlobalResultSet(final String store,
                           final String type,
                           final List<ErrorResultSet> failure,
                           final List<SuccessResultSet<K, V>> success) {
        this(store, type, null, failure, success);
    }

    /**
     * Creates a new {@link GlobalResultSet} instance.
     *
     * @param store   the name of the store.
     * @param type    the type of the store.
     * @param type    the global error message.
     * @param failure the error record set.
     * @param success the result record set.
     */
    @JsonCreator
    public GlobalResultSet(@JsonProperty("store") final String store,
                           @JsonProperty("type") final String type,
                           @JsonProperty("error") final String error,
                           @JsonProperty("failure") final List<ErrorResultSet> failure,
                           @JsonProperty("success") final List<SuccessResultSet<K, V>> success) {
        this.store = store;
        this.type = type;
        this.total = computeTotal(success);
        this.success = success;
        this.failure = failure;
        this.error = error;
    }

    private Long computeTotal(final List<SuccessResultSet<K, V>> success) {
        return Optional.ofNullable(success)
            .map(l -> l.stream().map(SuccessResultSet::getTotal).reduce(0L,  Long::sum))
            .orElse(0L);
    }

    public String getError() {
        return error;
    }

    public String getStore() {
        return store;
    }

    public String getType() {
        return type;
    }

    public Long getTotal() {
        return total;
    }

    public List<ErrorResultSet> getFailure() {
        return failure;
    }

    public List<SuccessResultSet<K, V>> getSuccess() {
        return success;
    }

    public List<Either<SuccessResultSet<K, V>, ErrorResultSet>> unwrap() {
        return Stream
            .of(unwrapError(), unwrapSuccess())
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<Either<SuccessResultSet<K, V>, ErrorResultSet>> unwrapError() {
        if (failure == null) return Collections.emptyList();
        return failure.stream()
                .map(Either::<SuccessResultSet<K, V>, ErrorResultSet>right)
                .collect(Collectors.toList());
    }

    private List<Either<SuccessResultSet<K, V>, ErrorResultSet>> unwrapSuccess() {
        if (success == null) return Collections.emptyList();
        return success.stream()
                .map(Either::<SuccessResultSet<K, V>, ErrorResultSet>left)
                .collect(Collectors.toList());
    }
}
