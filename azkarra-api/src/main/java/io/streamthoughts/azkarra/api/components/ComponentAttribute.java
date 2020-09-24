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
package io.streamthoughts.azkarra.api.components;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ComponentAttribute {

    private final String name;
    private final Map<String, Object> values;
    private final Map<String, Object> defaultValues;

    /**
     * Creates a new {@link ComponentAttribute} instance.
     *
     * @param name           the attribute name.
     */
    public ComponentAttribute(final String name) {
        this(name, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a new {@link ComponentAttribute} instance.
     *
     * @param name           the attribute name.
     * @param values         the attribute values.
     * @param defaultValues  the attribute default values.
     */
    public ComponentAttribute(final String name,
                              final Map<String, Object> values,
                              final Map<String, Object> defaultValues) {
        this.name = name;
        this.values = values;
        this.defaultValues = defaultValues;
    }

    public void add(final String member, final Object value, final Object defaultValue) {
        values.put(member, value);
        defaultValues.put(member, defaultValue);
    }

    public void add(final String member, final Object value) {
        add(member, value, null);
    }

    public String name() {
        return name;
    }

    public Object value(final String member) {
        return Optional.ofNullable(values.get(member)).orElse(defaultValues.getOrDefault(member, null));
    }

    public String stringValue(final String member) {
        return Optional.ofNullable(value(member)).map(Object::toString).orElse(null);
    }

    public boolean contains(final String member) {
        return values.containsKey(member) || defaultValues.containsKey(member);
    }

    public boolean contains(final String member, final Object value) {
        return Optional.ofNullable(value(member)).map(value::equals).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentAttribute)) return false;
        ComponentAttribute that = (ComponentAttribute) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(values, that.values) &&
                Objects.equals(defaultValues, that.defaultValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, values, defaultValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[name=" + name + ", values=" + values + ", defaults=" + defaultValues + "]";
    }
}
