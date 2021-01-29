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
package io.streamthoughts.azkarra.http.query;

import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.query.GenericQueryParams;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.StoreType;
import io.streamthoughts.azkarra.http.error.InvalidStateStoreQueryException;
import io.streamthoughts.azkarra.http.serialization.json.JsonQuerySerde;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonQuerySerdeTest {

    @Test
    public void shouldThrowExceptionWhenDeserializeGivenInvalidJson() {
        assertThrows(InvalidStateStoreQueryException.class, () -> {
            JsonQuerySerde.deserialize("store", "".getBytes());
        });
    }

    @Test
    public void shouldThrowExceptionWhenDeserializeGivenJsonWithNoType() {
        InvalidStateStoreQueryException exception = assertThrows(InvalidStateStoreQueryException.class, () -> {
            String dataString = " { \"query\" : {  \"get\" : {\"key\" : \"foo\"} } }";
            JsonQuerySerde.deserialize("store", dataString.getBytes());
        });

        assertEquals("Invalid JSON query: missing 'type' field", exception.getMessage());
    }

    @Test
    public void shouldDeserializeGivenValidJsonQueryWithNoOption() {

        String dataString = " { \"type\" : \"key_value\", \"query\" : {  \"get\" : {\"key\" : \"foo\"} } }";
        Tuple<QueryRequest, QueryOptions> tuple = JsonQuerySerde.deserialize("store", dataString.getBytes());
        assertNotNull(tuple);
        assertEquals(StoreType.KEY_VALUE, tuple.left().getStoreType());
        assertEquals(StoreOperation.GET, tuple.left().getStoreOperation());
        assertTrue(tuple.left().getParams().contains("key"));
    }

    @Test
    public void shouldDeserializeGivenValidJsonQueryWithOptions() {

        String dataString = " { \"type\" : \"key_value\", \"query\" : {  \"get\" : {\"key\" : \"foo\"} }, \"set_options\" : {\"retries\": 42, \"retry_backoff_ms\": 42} }";
        Tuple<QueryRequest, QueryOptions> tuple = JsonQuerySerde.deserialize("store", dataString.getBytes());
        assertNotNull(tuple);
        assertEquals(StoreType.KEY_VALUE, tuple.left().getStoreType());
        assertEquals(StoreOperation.GET, tuple.left().getStoreOperation());
        assertTrue(tuple.left().getParams().contains("key"));
        assertEquals(42, tuple.right().retries());
        assertEquals(42, tuple.right().retryBackoff().toMillis());
    }

    @Test
    public void shouldThrowExceptionGivenJsonQueryWithInvalidStoreType() {
        InvalidStateStoreQueryException exception = assertThrows(InvalidStateStoreQueryException.class, () -> {
            String dataString = " { \"type\" : \"invalid\", \"query\" : {  \"get\" : {\"key\" : \"foo\"} }, \"set_options\" : {} }";
            JsonQuerySerde.deserialize("store", dataString.getBytes());
        });

        assertEquals("Invalid store type: invalid", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionGivenJsonQueryWithMissingQuery() {
        InvalidStateStoreQueryException exception = assertThrows(InvalidStateStoreQueryException.class, () -> {
            String dataString = " { \"type\" : \"key_value\"}";
            JsonQuerySerde.deserialize("store", dataString.getBytes());
        });

        assertEquals("Invalid JSON query: missing 'query' clause", exception.getMessage());
    }

    @Test
    public void shouldDeserializeJsonGivenQueryWhenNoParamsIsRequired() {
        QueryRequest qInfo = new QueryRequest("store", StoreType.KEY_VALUE, StoreOperation.GET, GenericQueryParams.empty());
        String json = JsonQuerySerde.serialize(qInfo, QueryOptions.locally());

        assertEquals(
        "{" +
            "\"query\":{\"get\":{}}," +
            "\"type\":\"key_value\"," +
            "\"set_options\":{" +
            "\"retries\":0,\"retry_backoff_ms\":0,\"query_timeout_ms\":0,\"remote_access_allowed\":false,\"limit\":-1}}"
            , json);
    }
}