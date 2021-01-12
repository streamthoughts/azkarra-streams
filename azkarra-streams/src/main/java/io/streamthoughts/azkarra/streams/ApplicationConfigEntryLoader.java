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

import io.streamthoughts.azkarra.api.config.ConfEntry;

public interface ApplicationConfigEntryLoader {

    /**
     * Checks whether this loader supported the specified root config key.
     *
     * @param configKey the entry config key.
     * @return          {@code true} of the method {@link #load(ConfEntry, AzkarraApplication)}
     *                   can be invoked with the value config for the given key
     */
    boolean accept(final String configKey);

    /**
     * Loads the configuration key/value entry.
     *
     * @param configEntryObject    the configuration entry key/value object.
     * @param application       the {@link AzkarraApplication} instance.
     */
    void load(final ConfEntry configEntryObject,
              final AzkarraApplication application);
}
