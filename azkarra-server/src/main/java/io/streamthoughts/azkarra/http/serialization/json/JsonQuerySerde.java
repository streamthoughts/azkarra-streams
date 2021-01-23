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

import com.fasterxml.jackson.databind.JsonNode;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.query.GenericQueryParams;
import io.streamthoughts.azkarra.api.query.QueryOptions;
import io.streamthoughts.azkarra.api.query.QueryParams;
import io.streamthoughts.azkarra.api.query.QueryRequest;
import io.streamthoughts.azkarra.api.query.StoreOperation;
import io.streamthoughts.azkarra.api.query.StoreType;
import io.streamthoughts.azkarra.http.data.QueryOptionsRequest;
import io.streamthoughts.azkarra.http.error.InvalidStateStoreQueryException;
import io.streamthoughts.azkarra.serialization.SerializationException;
import io.streamthoughts.azkarra.serialization.json.Json;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * Class which is used to serialize / deserialize a JSON query.
 */
public class JsonQuerySerde {

    private static final String QUERY_JSON_FIELD = "query";
    private static final String QUERY_TYPE_JSON_FIELD = "type";
    private static final String SET_OPTIONS_JSON_FIELD = "set_options";

    private static final Json JSON = Json.getDefault();

    public static Tuple<QueryRequest, QueryOptions> deserialize(final String storeName, final byte[] data) {

        try {
            JsonNode jsonNode = JSON.deserialize(data);

            if (!jsonNode.has(QUERY_TYPE_JSON_FIELD)) {
                throw new InvalidStateStoreQueryException("Invalid JSON query: missing 'type' field");
            }

            String jsonType = jsonNode.get(QUERY_TYPE_JSON_FIELD).asText();

            Optional<StoreType> optionalStoreType = StoreType.parse(jsonType);
            if (optionalStoreType.isEmpty()) {
                throw new InvalidStateStoreQueryException("Invalid store type: " + jsonType);
            }

            final StoreType storeType = optionalStoreType.get();

            JsonNode jsonQuery = jsonNode.get(QUERY_JSON_FIELD);
            if (jsonQuery == null) {
                throw new InvalidStateStoreQueryException("Invalid JSON query: missing 'query' clause");
            }

            Iterator<Map.Entry<String, JsonNode>> clauses = jsonQuery.fields();
            List<Tuple<StoreOperation, QueryParams>> queries = new ArrayList<>();

            while (clauses.hasNext()) {
                Map.Entry<String, JsonNode> entry = clauses.next();

                String clause = entry.getKey();

                Optional<StoreOperation> storeOperation = StoreOperation.parse(clause);
                if (storeOperation.isEmpty()) {
                    throw new InvalidStateStoreQueryException("Invalid query operation: " + clause);
                }

                Map<String, Object> params = StreamSupport
                        .stream(spliteratorUnknownSize(entry.getValue().fields(), 0), false)
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> getJsonNodeValue(e.getValue())));

                queries.add(Tuple.of(storeOperation.get(), new GenericQueryParams(params)));
            }

            final JsonNode optionNode = jsonNode.get(SET_OPTIONS_JSON_FIELD);

            final QueryOptionsRequest options = optionNode == null ?
                null :
                    JSON.deserialize(optionNode, QueryOptionsRequest.class);

            final QueryRequest queryObject = new QueryRequest(
                storeName,
                storeType,
                queries.get(0).left(),
                queries.get(0).right());
            // only support a single query
            return Tuple.of(queryObject, newQueried(options));

        } catch (final SerializationException e) {
            throw new InvalidStateStoreQueryException("Invalid JSON query: " + e.getMessage(), e);
        }
    }

    private static Object getJsonNodeValue(final JsonNode jsonNode) {
        if (jsonNode.isBoolean())
            return jsonNode.asBoolean();
        if (jsonNode.isLong())
            return jsonNode.asLong();
        if (jsonNode.isInt())
            return jsonNode.asInt();
        if (jsonNode.isDouble())
            return jsonNode.doubleValue();

        return jsonNode.asText();
    }

    public static String serialize(final QueryRequest query, final QueryOptions options) {

        Map<String, Object> json = new HashMap<>();
        json.put(QUERY_TYPE_JSON_FIELD, query.getStoreType().prettyName());
        json.put(QUERY_JSON_FIELD, Collections.singletonMap(
            query.getStoreOperation().prettyName(), query.getParams().getAsMap())
        );
        json.put(SET_OPTIONS_JSON_FIELD, new QueryOptionsRequest(
            options.retries(),
            options.retryBackoff().toMillis(),
            options.queryTimeout().toMillis(),
            options.remoteAccessAllowed(),
            options.limit()
        ));
        return JSON.serialize(json);
    }

    private static QueryOptions newQueried(final QueryOptionsRequest options) {
        return options == null ?
            QueryOptions.immediately() :
            new QueryOptions(
                Optional.ofNullable(options.getRetries()).orElse(0),
                Duration.ofMillis(Optional.ofNullable(options.getRetryBackoff()).orElse(0L)),
                Duration.ofMillis(Optional.ofNullable(options.getQueryTimeout()).orElse(0L)),
                Optional.ofNullable(options.isRemoteAccessAllowed()).orElse(true),
                Optional.ofNullable(options.limit()).orElse(-1L)
            );
    }
}
