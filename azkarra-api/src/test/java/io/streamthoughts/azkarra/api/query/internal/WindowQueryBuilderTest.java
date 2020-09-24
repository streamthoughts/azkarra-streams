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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WindowQueryBuilderTest {

    public static final String STORE_NAME = "STORE_NAME";

    @Test
    public void shouldThrowInvalidQueryWhenBuildingWindowFetchGivenNoKeyParam() {
        final Query query = new WindowQueryBuilder(STORE_NAME).fetch();
        InvalidQueryException exception = assertThrows(InvalidQueryException.class, query::prepare);
        assertEquals(
                exception.getMessage(),
                "Missing requires parameters : [" +
                        WindowQueryBuilder.QUERY_PARAM_KEY + ", " +
                        WindowQueryBuilder.QUERY_PARAM_TIME +  "]");
    }

    @Test
    public void shouldThrowInvalidQueryWhenBuildingWindowFetchKeyRangeGivenNoKeyParam() {
        final Query query = new WindowQueryBuilder(STORE_NAME).fetchKeyRange();
        InvalidQueryException exception = assertThrows(InvalidQueryException.class, query::prepare);
        assertEquals(
                exception.getMessage(),
                "Missing requires parameters : [" +
                        WindowQueryBuilder.QUERY_PARAM_KEY_FROM + ", " +
                        WindowQueryBuilder.QUERY_PARAM_KEY_TO + ", " +
                        WindowQueryBuilder.QUERY_PARAM_TIME_FROM+ ", " +
                        WindowQueryBuilder.QUERY_PARAM_TIME_TO +  "]");
    }
}