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

package io.streamthoughts.azkarra.runtime;

import io.streamthoughts.azkarra.api.Executed;
import io.streamthoughts.azkarra.api.StreamsTopologyMeta;
import io.streamthoughts.azkarra.api.StreamsExecutionEnvironment;
import io.streamthoughts.azkarra.api.StreamsTopologyExecution;
import io.streamthoughts.azkarra.runtime.streams.topology.InternalExecuted;

import java.util.Objects;

public abstract class AbstractTopologyStreamsExecution<E extends StreamsExecutionEnvironment<E>>
        implements StreamsTopologyExecution {

    protected E environment;
    protected StreamsTopologyMeta meta;
    protected InternalExecuted executed;

    /**
     * Creates a new {@link AbstractTopologyStreamsExecution} instance.
     *
     * @param environment   the execution environment.
     * @param meta          the {@link StreamsTopologyMeta} meta-information to be executed.
     * @param executed      the execution options.
     */
    protected AbstractTopologyStreamsExecution(final E environment,
                                               final StreamsTopologyMeta meta,
                                               final Executed executed) {
        this.environment = Objects.requireNonNull(environment, "environment cannot be null");
        this.meta = Objects.requireNonNull(meta, "meta cannot be null");
        this.executed = new InternalExecuted(executed);
    }
}
