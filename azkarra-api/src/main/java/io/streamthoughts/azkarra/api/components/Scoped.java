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

import java.util.Objects;

public class Scoped {

    public static Scoped application() {
        return new Scoped(ComponentDescriptor.SCOPE_APPLICATION, "");
    }

    public static Scoped env(final String name) {
        return new Scoped(ComponentDescriptor.SCOPE_ENVIRONMENT, name);
    }

    public static Scoped streams(final String name) {
        return new Scoped(ComponentDescriptor.SCOPE_STREAMS, name);
    }

    private final String type;
    private final String name;

    /**
     * Creates a new {@link Scoped} instance.
     *
     * @param type  the scope type.
     * @param name  the scope name.
     */
    public Scoped(final String type, final String name) {
        this.type = type;
        this.name = name;
    }


    public String type() {
        return type;
    }

    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Scoped)) return false;
        Scoped scoped = (Scoped) o;
        return Objects.equals(type, scoped.type) &&
                Objects.equals(name, scoped.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[type=" + type + ", name=" + name + ']';
    }
}
