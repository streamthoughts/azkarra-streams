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

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * TopologyExecution provide a way for topology to be executed in an specific environment.
 *
 * @see StreamsExecutionEnvironment
 */
public interface StreamsTopologyExecution extends Callable<Optional<ApplicationId>> {

    /**
     * Starts the streams-topology encapsulated by this object.
     *
     * @return  the {@link ApplicationId}.
     */
    Optional<ApplicationId> start();

    /**
     * {@inheritDoc}
     */
    @Override
    default Optional<ApplicationId> call() {
        return start();
    }
}
