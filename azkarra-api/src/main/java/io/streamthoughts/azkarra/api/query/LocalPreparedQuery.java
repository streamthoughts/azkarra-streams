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

import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.monad.Validator;
import io.streamthoughts.azkarra.api.query.error.InvalidQueryException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@code LocalStoreQueryBuilder} is used to build new {@link LocalExecutableQuery}.
 *
 * @param <K>   the expected type for key.
 * @param <V>   the expected type for value.
 */
public interface LocalPreparedQuery<K, V> {

    /**
     * Gets a new {@link Validator} for the given parameters.
     *
     * @param params    the query parameters to validate.
     * @return          the {@link Validator}.
     */
    default Validator<QueryParams> validator(final QueryParams params) {
        return Validator.of(Objects.requireNonNull(params, "params cannot be null"));
    }

    /**
     * Validates the given query parameters.
     *
     * @param params    the query parameters to validate.
     * @return              the optional list of errors.
     */
    default Optional<List<Error>> validate(final QueryParams params) {
        Objects.requireNonNull(params, "parameters cannot be null");
        return validator(params)
            .toEither()
            .right()
            .toOptional();
    }

    /**
     * Builds a new {@link LocalExecutableQuery} based on the given {@link Query}.
     *
     * @param params    the parameters of the query.
     * @return          the new {@link LocalExecutableQuery}.
     */
    LocalExecutableQuery<K, V> compile(final QueryParams params) throws InvalidQueryException;

    final class MissingRequiredKeyError extends Error {

        private final String key;

        public static MissingRequiredKeyError of(final String key) {
            return new MissingRequiredKeyError(key);
        }

        MissingRequiredKeyError(final String key) {
            super("missing required parameter '" + key + "'.");
            this.key = key;
        }

        public String invalidKey() {
            return key;
        }
    }
}


