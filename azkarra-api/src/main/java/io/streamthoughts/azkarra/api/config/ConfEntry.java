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

package io.streamthoughts.azkarra.api.config;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ConfEntry {

    private final Property property;

    public static ConfEntry of(final String key, final Object value) {
        return new ConfEntry(key, value);
    }

    /**
     * Creates a new {@link ConfEntry} instance.
     * @param key       the object key.
     * @param value     the object value.
     */
    private ConfEntry(final String key, final Object value) {
        this.property = new Property(key, value);
    }

    /**
     * Gets the object key.
     *
     * @return  the string key.
     */
    public String key() {
        return property.key();
    }

    /**
     * Gets the object value.
     *
     * @return  the object value.
     */
    public Object value() {
        return property.get();
    }

    /**
     * Get the {@link ConfEntry} as {@link Property}.
     * @return  a new {@link Property} instance.
     */
    public Property asProperty() {
        return new Property(key(), value());
    }


    /**
     * Gets value as a string.
     */
    public String asString() {
        return property.getString(key());
    }

    /**
     * Gets valuer as a long.
     */
    public long asLong() {
        return property.getLong(key());
    }

    /**
     * Gets value as an integer.
     *
     * @return an integer value.
     */
    public int asInt() {
        return property.getInt(key());
    }

    /**
     * Gets value as an boolean.
     *
     * @return  the value as as boolean.
     */
    public boolean asBoolean() {
        return property.getBoolean(key());
    }

    /**
     * Gets value as a double.
     *
     * @return       the value as a double.
     */
    public double asDouble() {
        return property.getDouble(key());
    }

    /**
     * Gets value as a list.
     *
     * @return a string list value.
     */
    public List<String> asStringList() {
        return property.getStringList(key());
    }

    /**
     * Gets value as a {@link Conf}.
     *
     * @return a new {@link Conf} instance.
     */
    public Conf asSubConf() {
        return property.getSubConf(key());
    }

    /**
     * Gets value as a list of {@link Conf}.
     *
     * @return a new list of {@link Conf} instances.
     */
    public List<Conf> asSubConfList() {
        return property.getSubConfList(key());
    }

    /**
     * Gets value as a list of instances of type {@link T}.
     *
     * @param type      the class of the .
     * @param <T>       the expected type.
     *
     * @return  a new {@link Collection} of {@link T}.
     */
    public <T> Collection<T> getClasses(final Class<T> type) {
        return property.getClasses(key(), type);
    }

    /**
     * Gets value as an instances of type {@link T}.
     *
     * @param type      the class of the .
     * @param <T>       the expected type.
     *
     * @return  a new {@link Collection} of {@link T}.
     */
    public <T> T getClass( final Class<T> type) {
        return property.getClass(key(), type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfEntry)) return false;
        ConfEntry that = (ConfEntry) o;
        return Objects.equals(property, that.property);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(property);
    }
}
