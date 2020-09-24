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
package io.streamthoughts.azkarra.api.query.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamthoughts.azkarra.api.errors.Error;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A serializable error.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueryError extends Error implements Serializable {

    /**
     * Creates a new error instance from the specified {@link Throwable}.
     *
     * @param t the exception.
     *
     * @return a new {@link QueryError} instance
     */
    public static QueryError of(final Throwable t) {
        return new QueryError(t.getMessage());
    }

    public static List<QueryError> allOf(final List<Error> errors) {
        return errors.stream().map(QueryError::new).collect(Collectors.toList());
    }

    /**
     * Creates a new {@link QueryError} instance.
     *
     * @param message   the error message.
     */
    @JsonCreator
    public QueryError(@JsonProperty("message") final String message) {
        super(Objects.requireNonNull(message, "message cannot be null"));
    }

    @JsonProperty("message")
    public String message() {
        return super.message();
    }

    /**
     * Creates a new {@link QueryError} instance.
     *
     * @param error   the {@link Error}.
     */
    public QueryError(final Error error) {
        super(Objects.requireNonNull(error, "error cannot be null").message());
    }


    @Override
    public String toString() {
        return message();
    }
}
