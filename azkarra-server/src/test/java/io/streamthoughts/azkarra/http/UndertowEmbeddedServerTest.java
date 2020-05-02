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

import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import io.streamthoughts.azkarra.serialization.json.AzkarraSimpleModule;
import io.streamthoughts.azkarra.serialization.json.ConfSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class UndertowEmbeddedServerTest {

    @Test
    public void shouldRegisterUserSpecifiedJacksonModule() {

        AzkarraContext context = DefaultAzkarraContext.create();
        context.registerComponent(Module.class, MyCustomModule::new);

        UndertowEmbeddedServer server = new UndertowEmbeddedServer(context, true);
        server.configure(Conf.empty());

        ObjectMapper mapper = ExchangeHelper.JSON.unwrap();
        Set<Object> registeredModuleIds = mapper.getRegisteredModuleIds();
        Assertions.assertEquals(2, registeredModuleIds.size());
        Assertions.assertTrue(registeredModuleIds.contains(AzkarraSimpleModule.class.getName()));
        Assertions.assertTrue(registeredModuleIds.contains(MyCustomModule.class.getName()));
    }

    public static class MyCustomModule extends SimpleModule {
        private static final String NAME = MyCustomModule.class.getSimpleName();

        MyCustomModule() {
            super(NAME, VersionUtil.parseVersion("1.0", null, null));
            addSerializer(Conf.class, new ConfSerializer());
        }
    }
}