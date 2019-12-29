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

public class Restriction {

    public static final String TYPE_APPLICATION = "application";

    public static final String TYPE_ENVIRONMENT = "env";

    public static final String TYPE_STREAMS = "streams";

    public static Restriction application() {
        return new Restriction(TYPE_APPLICATION, "");
    }

    public static Restriction env(final String name) {
        return new Restriction(TYPE_ENVIRONMENT, name);
    }

    public static Restriction streams(final String name) {
        return new Restriction(TYPE_STREAMS, name);
    }

    private final String type;
    private final String name;

    /**
     * Creates a new {@link Restriction} instance.
     *
     * @param type  the scope type.
     * @param name  the scope name.
     */
    public Restriction(final String type, final String name) {
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
        if (!(o instanceof Restriction)) return false;
        Restriction scoped = (Restriction) o;
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
