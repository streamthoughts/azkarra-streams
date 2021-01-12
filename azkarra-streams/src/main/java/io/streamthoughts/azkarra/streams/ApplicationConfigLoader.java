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

package io.streamthoughts.azkarra.streams;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.ConfEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.streamthoughts.azkarra.streams.AzkarraApplication.AZKARRA_ROOT_CONFIG_KEY;

public class ApplicationConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationConfigLoader.class);

    private final List<ApplicationConfigEntryLoader> applicationConfigEntryLoaders;

    public ApplicationConfigLoader(final List<ApplicationConfigEntryLoader> applicationConfigEntryLoaders) {
        Objects.requireNonNull(applicationConfigEntryLoaders, "applicationConfigEntryLoaders cannot be null");
        this.applicationConfigEntryLoaders = applicationConfigEntryLoaders;
    }

    public void load(final AzkarraApplication application) {
        final Conf configuration =  application
            .getConfiguration()
            .getSubConf(AZKARRA_ROOT_CONFIG_KEY);

        final Set<String> keySet = configuration.keySet();
        for (String key : keySet) {
            final ConfEntry entry = ConfEntry.of(key, configuration.getValue(key));
            applicationConfigEntryLoaders.stream()
               .filter(applicationConfigEntryLoader -> applicationConfigEntryLoader.accept(key))
               .findFirst()
               .ifPresentOrElse(
                   applicationConfigEntryLoader -> {
                       LOG.debug("Loading application config with key: '{}'", key);
                       applicationConfigEntryLoader.load(entry, application);
                   },
                   () -> LOG.warn(
                       "Cannot found ApplicationConfigEntryLoader for key: {}. Configuration is ignored. ", key)
               );
        }
    }
}
