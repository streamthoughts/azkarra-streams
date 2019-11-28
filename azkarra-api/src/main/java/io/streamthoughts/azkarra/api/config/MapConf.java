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

import io.streamthoughts.azkarra.api.errors.InvalidConfException;
import io.streamthoughts.azkarra.api.errors.MissingConfException;
import io.streamthoughts.azkarra.api.monad.Tuple;
import io.streamthoughts.azkarra.api.util.TypeConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple {@link Conf} implementation which is backed by a {@link java.util.HashMap}.
 */
public class MapConf extends AbstractConf {

    private final Map<String, ?> parameters;

    private final Conf fallback;

    /**
     * Static helper that can be used to creates a new empty {@link MapConf} instance.
     *
     * @return a new {@link MapConf} instance.
     */
    static MapConf empty() {
        return new MapConf(Collections.emptyMap(), null, true);
    }

    /**
     * Static helper that can be used to creates a new single key-pair {@link MapConf} instance.
     *
     * @return a new {@link MapConf} instance.
     */
    static MapConf singletonConf(final String key, final Object value) {
        return new MapConf(Collections.singletonMap(key, value), null, true);
    }

    /**
     * Creates a new {@link MapConf} instance.
     *
     * @param parameters  the parameters configuration.
     */
    MapConf(final Map<String, ?> parameters) {
        this(parameters, null, true);
    }

    /**
     * Creates a new {@link MapConf} instance.
     *
     * @param parameters  the parameters configuration.
     */
    MapConf(final Map<String, ?> parameters,
            final boolean explode) {
        this(parameters, null, explode);
    }

    private MapConf(final Map<String, ?> parameters,
                    final Conf fallback,
                    final boolean explode) {
        Objects.requireNonNull(parameters, "parameters Conf cannot be null");
        this.parameters = explode ? explode(parameters).unwrap() : parameters;
        this.fallback =  fallback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(final String path) {
        final Object o = findForPathOrThrow(path, Conf::getString);
        try {
            return TypeConverter.getString(o);
        } catch (final IllegalArgumentException e) {
            throw new InvalidConfException(
                "Type mismatch for path '" + path + "': " + o.getClass().getSimpleName() + "<> String");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(final String path) {
        final Object o = findForPathOrThrow(path, Conf::getLong);
        try {
            return TypeConverter.getLong(o);
        } catch (final IllegalArgumentException e) {
            throw new InvalidConfException(
                "Type mismatch for path '" + path + "': " + o.getClass().getSimpleName() + "<> Long");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(final String path) {
        final Object o = findForPathOrThrow(path, Conf::getInt);
        try {
            return TypeConverter.getInt(o);
        } catch (final IllegalArgumentException e) {
            throw new InvalidConfException(
                "Type mismatch for path '" + path + "': " + o.getClass().getSimpleName() + "<> Integer");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(final String path) {
        final Object o = findForPathOrThrow(path, Conf::getBoolean);
        try {
            return TypeConverter.getBool(o);
        } catch (final IllegalArgumentException e) {
            throw new InvalidConfException(
                "Type mismatch for path '" + path + "': " + o.getClass().getSimpleName() + "<> Boolean");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(final String path) {
        final Object o = findForPathOrThrow(path, Conf::getDouble);
        try {
            return TypeConverter.getDouble(o);
        } catch (final IllegalArgumentException e) {
            throw new InvalidConfException(
                "Type mismatch for path '" + path + "': " + o.getClass().getSimpleName() + "<> Double");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getStringList(final String path) {
        Object o = findForPathOrThrow(path, Conf::getStringList);
        try {
            return (List<String>) TypeConverter.getList(o);
        } catch (final IllegalArgumentException e) {
            throw new InvalidConfException(
                "Type mismatch for path '" + path + "': " + o.getClass().getSimpleName() + "<> List");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf getSubConf(final String path) {
        Conf conf = (Conf) findForPathOrThrow(path, Conf::getSubConf);
        if (fallback != null && fallback.hasPath(path)) {
            conf = conf.withFallback(fallback.getSubConf(path));
        }
        return conf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Conf> getSubConfList(final String path) {
        return (List<Conf>) findForPathOrThrow(path, Conf::getSubConfList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPath(final String path) {
        String[] composed = splitPath(path);
        final String key = composed[0];

        boolean result = false;
        if (hasKey(key)) {
            if (composed.length > 1){
                final String nextPath = composed[1];
                result = getSubConf(key).hasPath(nextPath);
            } else {
                result = true;
            }
        }

        if (!result && fallback != null) {
            result = fallback.hasPath(path);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conf withFallback(final Conf fallback) {
        if (this.fallback == null) {
            return new MapConf(parameters, fallback, false);
        }
        return new MapConf(parameters, this.fallback.withFallback(fallback), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfAsMap() {
        Map<String, Object> map = new HashMap<>();
        if (fallback != null) {
            map.putAll(flatten(fallback.getConfAsMap()));
        }
        map.putAll(flatten(parameters));
        return Collections.unmodifiableMap(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getConfAsProperties() {
        Properties properties = new Properties();
        properties.putAll(getConfAsMap());
        return properties;
    }
    private boolean hasKey(final String key) {
        return parameters.containsKey(key);
    }

    private Object findForPathOrThrow(final String key, final BiFunction<Conf, String, Object> getter) {
        final Object result = findForPathOrGetNull(key, getter);
        if (result == null) {
            throw new MissingConfException(key);
        }
        return result;
    }

    private Object findForPathOrGetNull(final String path, final BiFunction<Conf, String, Object> getter) {
        final String[] composed = splitPath(path);
        final String key = composed[0];

        Object result = null;
        if (hasKey(key)) {
            if (composed.length > 1) {
                final String nextPath = composed[1];
                Conf subConf = getSubConf(key);
                // check if next key exist before invoking getter - otherwise this can throw MissingException.
                if (subConf.hasPath(splitPath(nextPath)[0])) {
                    result = getter.apply(subConf, nextPath);
                }
            } else {
                result = parameters.get(key);
            }
        }
        // check if next key exist before invoking getter - otherwise this can throw MissingException.
        if (result == null &&  fallback != null && fallback.hasPath(key)) {
            result = getter.apply(fallback, path);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "MapConf{" +
                "parameters=" + parameters +
                ", fallback=" + fallback +
                "} ";
    }

    @SuppressWarnings("unchecked")
    private static MapConf explode(final Map<String, ?> map) {
        final Stream<Tuple<String, ?>> tupleStream = map.entrySet()
            .stream()
            .map(Tuple::of)
            .map(tuple -> {
                if (tuple.left().contains(".")) {
                    final String[] split = splitPath(tuple.left());
                    final MapConf nested = explode(Collections.singletonMap(split[1], tuple.right()));
                    return new Tuple<>(split[0], nested);
                }
                else if (tuple.right() instanceof Map) {
                    return tuple.mapValue(m -> explode((Map)m));
                }
                return tuple;
            });
        final Map<String, Object> merged = tupleStream
                .collect(Collectors.toMap(Tuple::left, Tuple::right, MapConf::merge));
        return new MapConf(merged, false);
    }

    private static Object merge(final Object o1, final Object o2) {
        try {
            final Set<? extends Map.Entry<String, ?>> e1 = ((MapConf) o1).unwrap().entrySet();
            final Set<? extends Map.Entry<String, ?>> e2 = ((MapConf) o2).unwrap().entrySet();
            Map<String, Object> collected = Stream.of(e1, e2)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, MapConf::merge));
            if (collected.size() == 1) {
                Map.Entry<String, Object> entry = collected.entrySet().stream().findFirst().get();
                return MapConf.singletonConf(entry.getKey(), entry.getValue());
            }
            return new MapConf(collected, false);

        } catch (ClassCastException e) {
            throw new InvalidConfException(
                String.format(
                    "Cannot merge two parameters with different type : %s<>%s",
                    o1.getClass().getName(),
                    o2.getClass().getName()
                )
            );
        }
    }

    private static Map<String, Object> flatten(final Map<String, ?> map) {
        return map.entrySet().stream()
            .map(Tuple::of)
            .flatMap(MapConf::flatten)
            .collect(Collectors.toMap(Tuple::left, Tuple::right));
    }

    @SuppressWarnings("unchecked")
    private static Stream<Tuple<String, ?>> flatten(final Tuple<String, ?> entry) {
        final String k = entry.left();
        final Object v = entry.right();

        Set<? extends Map.Entry<String, ?>> nested = null;

        if (v instanceof Map) {
            nested = ((Map<String, ?>) v).entrySet();
        }
        if (v instanceof Conf) {
            nested = ((Conf) v).getConfAsMap().entrySet();
        }

        if (nested != null) {
            return nested.stream()
                .map(Tuple::of)
                .map(t -> prefixKeyWith(k, t))
                .flatMap(MapConf::flatten);
        }
        return Stream.of(entry);
    }


    private static String[] splitPath(final String key) {
        return key.split("\\.", 2);
    }

    private static <R> Tuple<String, R> prefixKeyWith(final String prefix,
                                                      final Tuple<String, R> tuple) {
        return tuple.mapKey(s -> prefix + '.' + s);
    }

    Map<String, ?> unwrap() {
        return parameters;
    }
}
