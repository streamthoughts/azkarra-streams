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

package io.streamthoughts.azkarra.api;

import io.streamthoughts.azkarra.api.config.Conf;

public interface StreamsExecutionEnvironmentFactory<T extends StreamsExecutionEnvironment<T>> {

    /**
     * Creates a new StreamsExecutionEnvironment from the specified name and an empty configuration.
     *
     * @param name  the environment name.
     * @return      a new {@link StreamsExecutionEnvironment} of type {@link T}.
     */
    default T create(final String name) {
        return create(name, Conf.empty());
    }

    /**
     * Creates a new StreamsExecutionEnvironment from the specified name and configuration.
     *
     * @param name  the environment name.
     * @param conf  the environment configuration.
     * @return      a new {@link StreamsExecutionEnvironment} of type {@link T}.
     */
    T create(final String name, final Conf conf);

    /**
     * Returns the string type associated with the {@link StreamsExecutionEnvironment} that can
     * be created from this factory.
     *
     * @return the type of the {@link StreamsExecutionEnvironment}.
     */
    String type();

}
