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
import io.streamthoughts.azkarra.api.server.AzkarraRestExtension;
import io.streamthoughts.azkarra.api.server.AzkarraRestExtensionContext;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

public class UndertowEmbeddedWithServletContainerIT {

    private static UndertowEmbeddedServer SERVER;

    @BeforeAll
    public static void setUpAll() {
        SERVER = new UndertowEmbeddedServer(
                DefaultAzkarraContext.create(),
                true
        );
        // Enable REST_EXTENSIONS to configure undertow with servlet handler
        Conf serverConfig = ServerConfBuilder.newBuilder().enableRestExtensions().build();
        SERVER.configure(serverConfig);
        SERVER.start();
    }

    @AfterAll
    public static void tearDownAll() {
        SERVER.stop();
    }

    @Test
    public void shouldDiscoverAndRegisterRestExtensionUsingJavaServiceLoader() {
        Collection<AzkarraRestExtension> registered = SERVER.getRegisteredExtensions();
        Assertions.assertEquals(1, registered.size());
        Assertions.assertEquals(registered.iterator().next().getClass(), TestAzkarraRestExtension.class);

        RestAssured.when().get("/test")
            .then()
            .assertThat()
            .statusCode(200)
            .contentType("application/json")
            .body("key", response -> equalTo("dummy"));
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static class TestResource {
        @GET
        @Path("/test")
        public Object get() {
            return Map.of("key", "dummy");
        }
    }

    public static class TestAzkarraRestExtension implements AzkarraRestExtension {

        @Override
        public void register(final AzkarraRestExtensionContext restContext) {
            restContext.configurable().register(TestResource.class);
        }

        @Override
        public void close() throws IOException {

        }
    }
}