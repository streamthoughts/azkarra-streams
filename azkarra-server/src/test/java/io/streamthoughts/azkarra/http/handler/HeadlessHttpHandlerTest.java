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
package io.streamthoughts.azkarra.http.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class HeadlessHttpHandlerTest {

    private static final String API_BASE_PATH = "/api/";

    @Test
    public void shouldAllowAccessForPostRequestWhenHeadlessEnable() throws Exception {
        assertRequestStatusCode(true, Methods.POST, "/info", StatusCodes.OK);
    }

    @Test
    public void shouldAllowAccessForApiPostStoresRequestWhenHeadlessEnable() throws Exception {
        assertRequestStatusCode(true, Methods.POST, "/api/applications/42/stores/", StatusCodes.OK);
    }

    @Test
    public void shouldAllowAccessForApiGetRequestWhenHeadlessEnable() throws Exception {
        assertRequestStatusCode(true, Methods.GET, API_BASE_PATH, StatusCodes.OK);
    }

    @Test
    public void shouldDenyAccessForApiPostRequestWhenHeadlessEnable() throws Exception {
        assertRequestStatusCode(true, Methods.POST, API_BASE_PATH, StatusCodes.FORBIDDEN);
    }

    @Test
    public void shouldDenyAccessForApiDeleteRequestWhenHeadlessEnable() throws Exception {
        assertRequestStatusCode(true, Methods.DELETE, API_BASE_PATH, StatusCodes.FORBIDDEN);
    }

    @Test
    public void shouldAllowAccessForApiDeleteRequestWhenHeadlessDisable() throws Exception {
        assertRequestStatusCode(false, Methods.DELETE, API_BASE_PATH, StatusCodes.OK);
    }

    @Test
    public void shouldAllowAccessForApiPostRequestWhenHeadlessDisable() throws Exception {
        assertRequestStatusCode(false, Methods.POST, API_BASE_PATH, StatusCodes.OK);
    }

    private void assertRequestStatusCode(final boolean headless,
                                         final HttpString method,
                                         final String relativePath,
                                         final int statusCode) throws Exception {

        final HttpHandler mkHandler = mock(HttpHandler.class);
        final HeadlessHttpHandler handler = new HeadlessHttpHandler(headless, mkHandler);

        final HttpServerExchange exchange = new HttpServerExchange(
            mock(ServerConnection.class),
            new HeaderMap(),
            new HeaderMap(),
            0L
        );
        exchange.setRequestMethod(method);
        exchange.setRelativePath(relativePath);

        handler.handleRequest(exchange);
        Assertions.assertEquals(exchange.getStatusCode(), statusCode);
    }
}