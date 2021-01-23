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
package io.streamthoughts.azkarra.api.query.internal;

import io.streamthoughts.azkarra.api.query.GenericQueryParams;
import io.streamthoughts.azkarra.api.query.LocalExecutableQuery;
import io.streamthoughts.azkarra.api.query.LocalPreparedQuery;
import io.streamthoughts.azkarra.api.query.error.InvalidQueryException;
import org.junit.jupiter.api.Test;

import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_FROM;
import static io.streamthoughts.azkarra.api.query.internal.QueryConstants.QUERY_PARAM_KEY_TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KeyValueQueryBuilderTest {

    public static final String STORE_NAME = "STORE_NAME";

    @Test
    public void shouldThrowInvalidQueryWhenBuildingKeyValueGetGivenNoKeyParam() {
        final LocalPreparedQuery query = new KeyValueQueryBuilder(STORE_NAME).get();
        InvalidQueryException exception = assertThrows(InvalidQueryException.class, () -> query.compile(new GenericQueryParams()));
        assertEquals(
                exception.getMessage(),
                "Missing requires parameters : [" + QUERY_PARAM_KEY + "]");
    }

    @Test
    public void shouldThrowInvalidQueryWhenBuildingKeyValueRangeGivenNoKeyParam() {
        final LocalPreparedQuery query = new KeyValueQueryBuilder(STORE_NAME).range();
        InvalidQueryException exception = assertThrows(InvalidQueryException.class, () -> query.compile(new GenericQueryParams()));
        assertEquals(
                exception.getMessage(),
                "Missing requires parameters : [" + QUERY_PARAM_KEY_FROM + ", " + QUERY_PARAM_KEY_TO + "]");
    }

    @Test
    public void shouldSuccessWhenBuildingKeyValueAllGivenNoParam() {
        final LocalPreparedQuery<Object, Object> query = new KeyValueQueryBuilder(STORE_NAME).all();
        LocalExecutableQuery<Object, Object> prepared = query.compile(new GenericQueryParams());
        assertNotNull(prepared);
    }

    @Test
    public void shouldSuccessWhenBuildingKeyValueGetGivenKeyParam() {
        final LocalPreparedQuery<Object, Object> query = new KeyValueQueryBuilder(STORE_NAME).get();
        LocalExecutableQuery<Object, Object> prepared = query.compile(new GenericQueryParams()
            .put(QUERY_PARAM_KEY, "key")
        );
        assertNotNull(prepared);
    }

    @Test
    public void shouldSuccessWhenBuildingKeyValueRangeGivenKeyParam() {
        final LocalPreparedQuery<Object, Object> query = new KeyValueQueryBuilder(STORE_NAME).range();
        LocalExecutableQuery<Object, Object> prepared = query.compile(new GenericQueryParams()
            .put(QUERY_PARAM_KEY_FROM, "keyFrom")
            .put(QUERY_PARAM_KEY_TO, "keyTo")
        );
        assertNotNull(prepared);
    }

}