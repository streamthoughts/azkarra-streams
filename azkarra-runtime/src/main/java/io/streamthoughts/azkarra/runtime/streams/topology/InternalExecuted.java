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
package io.streamthoughts.azkarra.runtime.streams.topology;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsLifecycleInterceptor;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.KafkaStreamsFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class InternalExecuted extends Executed {

    /**
     * Creates a new {@link InternalExecuted} instance.
     */
    public InternalExecuted() {
        super();
    }

    /**
     * Creates a new {@link InternalExecuted} instance.
     *
     * @param executed  the {@link Executed} instance.
     */
    public InternalExecuted(final Executed executed) {
        super(executed);
    }

    public String name() {
        return name;
    }

    public String nameOrElseGet(final String defaultValue) {
        return Optional.ofNullable(name).orElse(defaultValue);
    }

    public String descriptionOrElseGet(final String defaultValue) {
        return Optional.ofNullable(description).orElse(defaultValue);
    }

    public String description() {
        return description;
    }

    public Conf config() {
        return Optional.ofNullable(config).orElse(Conf.empty());
    }

    public List<Supplier<StreamsLifecycleInterceptor>> interceptors() {
        return interceptors;
    }

    public Optional<Supplier<KafkaStreamsFactory>> factory() {
        return Optional.ofNullable(factory);
    }
}
