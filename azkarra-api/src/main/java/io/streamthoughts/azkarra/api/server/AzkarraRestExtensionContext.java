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
package io.streamthoughts.azkarra.api.server;

import io.streamthoughts.azkarra.api.AzkarraContext;

import javax.ws.rs.core.Configurable;

/**
 * This interfaces provides the capability for {@link AzkarraRestExtension} implementations
 * to register JAX-RS resources using the provided {@link Configurable} and to get access to
 * the {@link AzkarraContext} instance.
 *
 * @see AzkarraRestExtension
 */
public interface AzkarraRestExtensionContext {

    /**
     * Provides an implementation of {@link javax.ws.rs.core.Configurable} that
     * must be used to register JAX-RS resources.
     *
     * @return the JAX-RS {@link javax.ws.rs.core.Configurable}.
     */
    Configurable<? extends Configurable> configurable();

    /**
     * Provides the {@link AzkarraContext} instance that can be used to retrieve registered components.
     *
     * @return  the {@link AzkarraContext} instance.
     */
    AzkarraContext context();
}
