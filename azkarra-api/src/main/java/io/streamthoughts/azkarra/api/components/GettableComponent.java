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
package io.streamthoughts.azkarra.api.components;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;

/**
 * Class for getting a configurable component.
 *
 * @param <T>   the component-type.
 */
public interface GettableComponent<T> extends AutoCloseable {

    /**
     * Gets the instance of type {@link T}, which may be shared or independent.
     *
     * @param conf  the configuration to be used if the component implement {@link Configurable}.
     *
     * @return  the component of type {@link T}.
     */
    T get(final Conf conf);

    /**
     * Gets the descriptor for the component.
     *
     * @return  the {@link ComponentDescriptor}.
     */
    ComponentDescriptor<T> descriptor();

    /**
     * Closes the created component.
     */
    void close();
}
