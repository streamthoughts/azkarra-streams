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
package io.streamthoughts.azkarra.streams.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import io.streamthoughts.azkarra.api.config.AbstractConf;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.errors.MissingConfException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The {@link AzkarraConf} is {@link Conf} implementation backed by the Typesafe {@link Config}.
 */
public class AzkarraConf extends AbstractConf {

    /**
     * Static helper that can be used to creates a new empty {@link AzkarraConf} instance.
     *
     * @return a new {@link AzkarraConf} instance.
     */
    public static AzkarraConf empty() {
        return new AzkarraConf(ConfigFactory.empty());
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraConf} instance
     * from the specified properties.
     *
     * @return a new {@link AzkarraConf} instance.
     */
    public static AzkarraConf create(final Properties config) {
        return new AzkarraConf(ConfigFactory.parseProperties(config, ConfigParseOptions.defaults()));
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraConf} instance
     * from the specified map.
     *
     * @return a new {@link AzkarraConf} instance.
     */
    public static AzkarraConf create(final Map<String,  ? extends Object> config) {
        return new AzkarraConf(ConfigFactory.parseMap(config));
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraConf} instance
     * using the specified resource base name.
     *
     * @return a new {@link AzkarraConf} instance.
     */
    public static AzkarraConf create(final String resourceBasename) {
        return new AzkarraConf(ConfigFactory.load(resourceBasename));
    }

    /**
     * Static helper that can be used to creates a new {@link AzkarraConf} instance
     * that loads a default configuration.
     *
     * <p>
     * This method loads the following (first-listed are higher priority):
     * <ul>
     *     <li>system properties</li>
     *     <li>application.conf (all resources on classpath with this name)</li>
     *     <li>application.json (all resources on classpath with this name)</li>
     *     <li>application.properties (all resources on classpath with this name)</li>
     *     <li>reference.conf (all resources on classpath with this name)</li>
     * </ul>
     * </p>
     *
     * @return a new {@link AzkarraConf} instance.
     */
    public static AzkarraConf create() {
        return new AzkarraConf(ConfigFactory.load());
    }

    private final Config config;

    /**
     * Creates a new {@link AzkarraConf} instance.
     *
     * @param config  the {@link Config} instance to used as default configuration.
     */
    protected AzkarraConf(final Config config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final String path) {
        if (!hasPath(path)) throw new MissingConfException(path);
        return config.getString(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(final String key) {
        if (!hasPath(key)) throw new MissingConfException(key);
        return config.getLong(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final String key) {
        if (!hasPath(key)) throw new MissingConfException(key);
        return config.getInt(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(final String key) {
        if (!hasPath(key)) throw new MissingConfException(key);
        return config.getBoolean(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(String key) {
        if (!hasPath(key)) throw new MissingConfException(key);
        return config.getDouble(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getStringList(final String key) {
        if (!hasPath(key)) throw new MissingConfException(key);
        return config.getStringList(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getSubConf(final String key) {
        if (!hasPath(key)) throw new MissingConfException(key);
        return new AzkarraConf(config.getConfig(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Conf> getSubConfList(final String key) {
        if (!hasPath(key)) throw new MissingConfException(key);
        return config.getConfigList(key)
                .stream()
                .map(AzkarraConf::new)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPath(final String key) {
        return config.hasPath(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf withFallback(final Conf defaults) {
        if (defaults instanceof AzkarraConf) {
            final Config typesafeConfg = ((AzkarraConf)defaults).config;
            return new AzkarraConf(config.withFallback(typesafeConfg));
        }

        return withFallback(AzkarraConf.create(defaults.getConfAsMap()));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfAsMap() {
        Map<String, Object> props = new HashMap<>();
        config.entrySet().forEach(e -> props.put(e.getKey(), config.getAnyRef(e.getKey())));
        return new TreeMap<>(props);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getConfAsProperties() {
        Properties properties = new Properties();
        config.entrySet().forEach(e -> properties.setProperty(e.getKey(), config.getString(e.getKey())));
        return properties;
    }
}
