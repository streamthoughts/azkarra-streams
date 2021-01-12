/*
 * Copyright 2019-2021 StreamThoughts.
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

package io.streamthoughts.azkarra.streams.config.loader;

import io.streamthoughts.azkarra.api.config.ConfEntry;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.streams.AbstractConfigEntryLoader;
import io.streamthoughts.azkarra.streams.AzkarraApplication;

import java.util.List;

public class ComponentConfigEntryLoader extends AbstractConfigEntryLoader {

    private static final String CONFIG_ENTRY_KEY = "components";

    /**
     * Creates a new {@link ComponentConfigEntryLoader} instance.
     */
    public ComponentConfigEntryLoader() {
        super(CONFIG_ENTRY_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final ConfEntry configObject, final AzkarraApplication application) {
        final List<String> components = configObject.asStringList();
        for (final String componentType : components) {
            try {
                application.getContext().registerComponent(Class.forName(componentType));
            } catch (ClassNotFoundException e) {
                throw new AzkarraException("Invalid configuration", e);
            }
        }
    }
}
