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
package io.streamthoughts.azkarra.api.config;

import io.streamthoughts.azkarra.api.errors.MissingConfException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Class which can be used for configuring components.
 *
 * @see io.streamthoughts.azkarra.api.AzkarraContext
 * @see io.streamthoughts.azkarra.api.StreamsExecutionEnvironment
 * @see io.streamthoughts.azkarra.api.streams.TopologyProvider
 */
public interface Conf {

    EmptyConf EMPTY = new EmptyConf();

    /**
     * Static helper that can be used to creates a new empty
     * {@link Conf} instance using the specified key-value pair.
     *
     * @return a new {@link Conf} instance.
     */
    static Conf with(final String path, final Object value) {
        return new Property(path, value);
    }

    /**
     * Static helper that can be used to creates a new empty
     * {@link Conf} instance using the specified {@link Map}.
     *
     * @return a new {@link Conf} instance.
     */
    static Conf with(final Map<String, ?> map) {
        return new MapConf(map);
    }

    /**
     * Static helper that can be used to creates a new empty {@link Conf} instance.
     *
     * @return a new {@link Conf} instance.
     */
    static Conf empty() {
        return EMPTY;
    }

    /**
     * Gets a required parameter a a string.
     *
     * @param path   the parameter path.
     * @return      the parameter value as a string.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     */
    String getString(final String path);

    /**
     * Gets an optional parameter as a string.
     *
     * @param path  the parameter path.
     * @return      the {@link Optional} value.
     */
    default Optional<String> getOptionalString(final String path) {
        return Optional.ofNullable(hasPath(path) ? getString(path) : null);
    }

    /**
     * Gets a required parameter as a long.
     *
     * @param path  the parameter path.
     * @return      the parameter value as a long.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     */
    long getLong(final String path);

    /**
     * Gets an optional parameter as a long.
     *
     * @param path   the parameter path.
     * @return       the {@link Optional} value.
     */
    default Optional<Long> getOptionalLong(final String path) {
        return Optional.ofNullable(hasPath(path) ? getLong(path) : null);
    }

    /**
     * Gets a required parameter as an integer.
     *
     * @param path   the parameter path.
     * @return       the parameter value as a int.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     */
    int getInt(final String path);

    /**
     * Gets an optional parameter as an integer.
     *
     * @param path   the parameter path.
     * @return       the {@link Optional} value.
     */
    default Optional<Integer> getOptionalInt(final String path) {
        return Optional.ofNullable(hasPath(path) ? getInt(path) : null);
    }

    /**
     * Gets a required parameter as an boolean.
     *
     * @param path   the parameter path.
     * @return       the parameter value as a boolean.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     */
    boolean getBoolean(final String path);

    /**
     * Gets an optional parameter as an boolean.
     *
     * @param path   the parameter path.
     * @return       the {@link Optional} value.
     */
    default Optional<Boolean> getOptionalBoolean(final String path) {
        return Optional.ofNullable(hasPath(path) ? getBoolean(path) : null);
    }

    /**
     * Gets a required parameter as a double.
     *
     * @param path   the parameter path.
     * @return       the parameter value as a double.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     */
    double getDouble(final String path);

    /**
     * Gets an optional parameter as an double.
     *
     * @param path   the parameter path.
     * @return       the {@link Optional} value.
     */
    default Optional<Double> getOptionalDouble(final String path) {
        return Optional.ofNullable(hasPath(path) ? getDouble(path) : null);
    }

    /**
     * Gets a required parameter as a list.
     *
     * @param path    the parameter path.
     *
     *
     * @return a string list value.
     */
    List<String> getStringList(final String path);

    /**
     * Gets a required parameter as a {@link Conf}.
     *
     * @param path    the parameter path.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     * @return a new {@link Conf} instance.
     */
    Conf getSubConf(final String path);

    /**
     * Gets a required parameter as a list of {@link Conf}.
     *
     * @param path     the parameter path.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     *
     * @return a new list of {@link Conf} instances.
     */
    List<Conf> getSubConfList(final String path);

    /**
     * Checks whether the specified path exists into this {@link Conf}.
     *
     * @param path      the path to be checked.
     * @return          {@code true} if the path exists, {@code false} otherwise.
     */
    boolean hasPath(final String path);

    Conf withFallback(final Conf fallback);

    /**
     * Gets a required parameter as a list of instances of type {@link T}.
     *
     * @param path      the parameter path.
     * @param type      the class of the .
     * @param <T>       the expected type.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     *
     * @return  a new {@link Collection} of {@link T}.
     */
    <T> Collection<T> getClasses(final String path, final Class<T> type);

    /**
     * Gets a required parameter as an instances of type {@link T}.
     *
     * @param path       the parameter path.
     * @param type      the class of the .
     * @param <T>       the expected type.
     *
     * @throws MissingConfException if no parameter can be found for the specified path.
     *
     * @return  a new {@link Collection} of {@link T}.
     */
    <T> T getClass(final String path, final Class<T> type);

    /**
     * Converts this {@link Conf} into a path-value map.
     *
     * @return  a new {@link Map} instance containing the configuration values.
     */
    Map<String, Object> getConfAsMap() ;

    /**
     * Converts this {@link Conf} into a properties.
     *
     * @return  a new {@link Properties} instance containing the configuration values.
     */
    Properties getConfAsProperties();
}
