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
package io.streamthoughts.azkarra.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.streamthoughts.azkarra.http.error.InvalidHttpQueryParamException;
import io.streamthoughts.azkarra.serialization.SerializationException;
import io.streamthoughts.azkarra.serialization.json.Json;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

/**
 * Simple class to wrap {@link HttpServerExchange} instance.
 */
public class ExchangeHelper {

    private static final String CONTENT_TYPE = "application/json; charset=utf-8";

    public static final Json JSON = new Json(new ObjectMapper());

    static {
        JSON.configure(om -> om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    /**
     * Static helper that can be used to get an optional param from query for the specified name.
     *
     * @param exchange  the {@link HttpServerExchange} instance.
     * @param name      the query param name.
     * @return          an {@link Optional}.
     */
    public static Optional<String> getOptionalQueryParam(final HttpServerExchange exchange, final String name) {
        return getFirst(name, exchange.getQueryParameters());
    }

    /**
     * Static helper that can be used to get an mandatory param from query for the specified name.
     *
     * @param exchange  the {@link HttpServerExchange} instance.
     * @param name      the query param name.
     * @return          a param as a string.
     *
     * @throws InvalidHttpQueryParamException if param is missing.
     */
    public static String getQueryParam(final HttpServerExchange exchange, final String name) {
        Optional<String> param = getOptionalQueryParam(exchange, name);
        if (param.isPresent()) {
            return param.get();
        } else {
            throw new InvalidHttpQueryParamException(name);
        }
    }

    /**
     * Static helper that can be used to read JSON object from HTTP-request payload.
     *
     * @param exchange  the {@link HttpServerExchange} instance.
     * @return          an {@link JsonNode} instance.
     *
     * @throws SerializationException if an error happens while de-serializing.
     */
    public static JsonNode readJsonRequest(final HttpServerExchange exchange) throws SerializationException {
        return JSON.deserialize(exchange.getInputStream());
    }

    /**
     * Static helper that can be used to read JSON object from HTTP-request payload.
     *
     * @param exchange  the {@link HttpServerExchange} instance.
     * @param type      the type.
     * @return          an {@link JsonNode} instance.
     *
     * @throws SerializationException if an error happens while de-serializing.
     */
    public static <T> T readJsonRequest(final HttpServerExchange exchange,
                                        final Class<T> type) throws SerializationException {
        return JSON.deserialize(exchange.getInputStream(), type);
    }

    /**
     * Static helper that can be used to write a JSON object to HTTP-response with {@link StatusCodes#OK}.
     *
     * @param exchange  the {@link HttpServerExchange} instance.
     * @param response  the {@link Object} to serialize.
     */
    public static void sendJsonResponse(final HttpServerExchange exchange, final Object response) {
        sendJsonResponseWithCode(exchange, response, StatusCodes.OK);
    }

    /**
     * Static helper that can be used to write a JSON object to HTTP-response with the specified status code.
     *
     * @param exchange   the {@link HttpServerExchange} instance.
     * @param response   the {@link Object} to serialize.
     * @param statusCode the status code.
     */
    public static void sendJsonResponseWithCode(final HttpServerExchange exchange,
                                                final Object response,
                                                final int statusCode) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, CONTENT_TYPE);
        exchange.getResponseSender().send(JSON.serialize(response), StandardCharsets.UTF_8);
    }

    private static Optional<String> getFirst(final String name,
                                             final Map<String, Deque<String>> parameters) {
        Deque<String> parameter = parameters.get(name);
        return Optional.ofNullable(parameter).map(Deque::getFirst);
    }
}
