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

package io.streamthoughts.azkarra.api.errors;

import io.streamthoughts.azkarra.api.components.Qualifier;

public class NoSuchComponentException extends NotFoundException {

    public static NoSuchComponentException forAlias(final String alias) {
        return new NoSuchComponentException(
            "No such component for alias '" + alias + "'");
    }

    public static NoSuchComponentException forAliasAndQualifier(final String alias, final Qualifier qualifier) {
        return new NoSuchComponentException(
            "No such component for alias '" + alias + "' and qualifier '" + qualifier + "'");
    }

    /**
     * Creates a new {@link NotFoundException} instance.
     *
     * @param message the error message.
     */
    public NoSuchComponentException(final String message) {
        super(message);
    }
}
