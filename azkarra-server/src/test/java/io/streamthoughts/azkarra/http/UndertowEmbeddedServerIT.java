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
package io.streamthoughts.azkarra.http;

import io.restassured.RestAssured;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import io.undertow.Handlers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

public class UndertowEmbeddedServerIT {

    private static UndertowEmbeddedServer SERVER;

    @BeforeAll
    public static void setUpAll() {
        SERVER = new UndertowEmbeddedServer(
                DefaultAzkarraContext.create(),
                true
        );

        SERVER.addRoutingHandler(service -> Handlers.routing().get("/error", exchange -> {
            throw new AzkarraException("Testing error");
        }));

        SERVER.configure(Conf.empty());
        SERVER.start();
    }

    @AfterAll
    public static void tearDownAll() {
        SERVER.stop();
    }

    @Test
    public void shouldHandleExceptionAndReturnJsonMessage() {
        RestAssured.when().get("/error")
            .then()
            .assertThat()
            .statusCode(500)
            .contentType("application/json")
            .body("message", response -> equalTo("Internal Azkarra Streams API Error : Testing error"))
            .body("error_code", response -> equalTo(500));
    }
}