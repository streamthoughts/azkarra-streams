/*
 * Copyright 2019 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy with the License at
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

import io.streamthoughts.azkarra.api.query.QueryParams;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class KeyValueQueryBuilderTest {

    public static final String STORE_NAME = "STORE_NAME";

    @Test
    public void shouldThrowInvalidQueryWhenBuildingKeyValueGetGivenNoKeyParam() {
        final Query query = new KeyValueQueryBuilder(STORE_NAME).get();
        InvalidQueryException exception = assertThrows(InvalidQueryException.class, query::prepare);
        assertEquals(
                exception.getMessage(),
                "Missing requires parameters : [" + KeyValueQueryBuilder.QUERY_PARAM_KEY + "]");
    }

    @Test
    public void shouldThrowInvalidQueryWhenBuildingKeyValueRangeGivenNoKeyParam() {
        final Query query = new KeyValueQueryBuilder(STORE_NAME).range();
        InvalidQueryException exception = assertThrows(InvalidQueryException.class, query::prepare);
        assertEquals(
                exception.getMessage(),
                "Missing requires parameters : [" + KeyValueQueryBuilder.QUERY_PARAM_KEY_FROM+ ", " + KeyValueQueryBuilder.QUERY_PARAM_KEY_TO + "]");
    }

    @Test
    public void shouldSuccessWhenBuildingKeyValueAllGivenNoParam() {
        final Query<Object, Object> query = new KeyValueQueryBuilder(STORE_NAME).all();
        PreparedQuery<Object, Object> prepared = query.prepare();
        assertNotNull(prepared);
    }

    @Test
    public void shouldSuccessWhenBuildingKeyValueGetGivenKeyParam() {
        final Query<Object, Object> query = new KeyValueQueryBuilder(STORE_NAME).get();
        PreparedQuery<Object, Object> prepared = query.prepare(new QueryParams(new HashMap<>() {{
            put(KeyValueQueryBuilder.QUERY_PARAM_KEY, "key");
        }}));
        assertNotNull(prepared);
    }

    @Test
    public void shouldSuccessWhenBuildingKeyValueRangeGivenKeyParam() {
        final Query<Object, Object> query = new KeyValueQueryBuilder(STORE_NAME).range();
        PreparedQuery<Object, Object> prepared = query.prepare(new QueryParams(new HashMap<>() {{
            put(KeyValueQueryBuilder.QUERY_PARAM_KEY_FROM, "keyFrom");
            put(KeyValueQueryBuilder.QUERY_PARAM_KEY_TO, "keyTo");
        }}));
        assertNotNull(prepared);
    }

}