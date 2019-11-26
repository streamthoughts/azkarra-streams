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

/**
 * Factory to create new {@link ComponentDescriptor} instance.
 *
 * @param <T>   the provider type.
 */
public interface ComponentDescriptorFactory<T> {

    /**
     * Makes a new {@link ComponentDescriptor} instance.
     *
     * @param type      the component class.
     * @param version   the provider version.
     *
     * @return          a new instance of {@link ComponentDescriptor}.
     */
    ComponentDescriptor<T> make(final Class<? extends T> type, final String version);
}