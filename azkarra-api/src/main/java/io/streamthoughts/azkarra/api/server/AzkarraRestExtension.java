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
package io.streamthoughts.azkarra.api.server;

import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;

/**
 * A pluggable interface to allow registration of new JAX-RS resources like REST endpoints.
 * The implementations are discovered using the standard Java {@link java.util.ServiceLoader} mechanism.
 *
 * Hence, the fully qualified name of the extension classes that implement the {@link AzkarraRestExtension}
 * interface must be add to a {@code META-INF/services/io.streamthoughts.azkarra.api.server.AzkarraRestExtension} file.
 */
public interface AzkarraRestExtension extends Configurable, AutoCloseable {

    /**
     * Configures this instance with the specified {@link Conf}.
     * The configuration passed to the method correspond used for configuring the {@link EmbeddedHttpServer}
     *
     * @param configuration  the {@link Conf} instance used to configure this instance.
     */
    @Override
    default void configure(final Conf configuration) {

    }

    /**
     * The {@link AzkarraRestExtension} implementations should use this method to register JAX-RS resources.
     * @param restContext   the {@link AzkarraRestExtensionContext} instance.
     */
    void register(final AzkarraRestExtensionContext restContext);

    @Override
    default void close() throws Exception {

    }
}
