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
package io.streamthoughts.azkarra.api.query.error;

import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.errors.Error;
import io.streamthoughts.azkarra.api.query.LocalPreparedQuery;

import java.util.List;
import java.util.stream.Collectors;

public class InvalidQueryException extends AzkarraException {

    /**
     * Creates a new {@link InvalidQueryException} instance.
     *
     * @param errors    the list of errors.
     */
    public InvalidQueryException(final List<Error> errors) {
        this(createExceptionMessage(errors));
    }

    /**
     * Creates a new {@link InvalidQueryException} instance.
     *
     * @param message   the error message.
     */
    public InvalidQueryException(final String message) {
        super(message);
    }

    private static String createExceptionMessage(final List<Error> errors) {
        final boolean isAllMissingRequiredKey = errors
                .stream()
                .allMatch(e -> e instanceof LocalPreparedQuery.MissingRequiredKeyError);
        if (isAllMissingRequiredKey) {
            final String missing = errors.stream()
                    .map(e -> (LocalPreparedQuery.MissingRequiredKeyError) e)
                    .map(LocalPreparedQuery.MissingRequiredKeyError::invalidKey)
                    .collect(Collectors.joining(", "));
            return "Missing requires parameters : [" + missing + "]";
        }

        final String causes = errors.stream().map(Error::message).collect(Collectors.joining(", "));
        return "Invalid query parameters : [" + causes + "]";
    }
}
