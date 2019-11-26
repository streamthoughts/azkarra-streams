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
package io.streamthoughts.azkarra.streams.components;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.components.ComponentClassReader;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import io.streamthoughts.azkarra.runtime.components.DefaultProviderClassReader;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link ClasspathComponentScanner} class can be used used to scan the classpath for automatically
 * registering declared classes annotated with {@link Component} and {@link ComponentFactory} classes.
 */
public class ClasspathComponentScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathComponentScanner.class);

    private final ComponentClassReader reader;
    private final ComponentRegistry registry;

    private FilterBuilder filterBuilder;

    /**
     * Creates a new {@link ClasspathComponentScanner} instance.
     *
     * @param reader  the {@link DefaultProviderClassReader} used to register providers.
     */
    public ClasspathComponentScanner(final ComponentClassReader reader, final ComponentRegistry registry) {
        Objects.requireNonNull(reader, "reader cannot be null");
        Objects.requireNonNull(reader, "registry cannot be null");
        this.reader = reader;
        this.registry = registry;
        this.filterBuilder = new FilterBuilder();
    }

    public void scan() {
        final URL[] urls = ClasspathHelper.forJavaClassPath().toArray(new URL[0]);
        scanUrlsForComponents(urls, ClasspathComponentScanner.class.getClassLoader());
    }

    public void scan(final Package source) {
        final URL[] urls = ClasspathHelper.forPackage(source.getName()).toArray(new URL[0]);
        filterBuilder.includePackage(source.getName());
        scanUrlsForComponents(urls, ClasspathComponentScanner.class.getClassLoader());
    }

    private void scanUrlsForComponents(final URL[] urls,
                                       final ClassLoader classLoader) {
        LOG.info("Loading providers from classpath : '{}'\n\t",
                Arrays.stream(urls).map(URL::getPath).collect(Collectors.joining("\n\t")));

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setClassLoaders(new ClassLoader[]{classLoader});
        builder.addUrls(urls);
        builder.filterInputsBy(filterBuilder);
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());

        builder.useParallelExecutor();

        Reflections reflections = new Reflections(builder);

        registerAllDeclaredComponentFactories(reflections);
        registerAllDeclaredComponents(reflections);
    }

    private void registerAllDeclaredComponents(final Reflections reflections) {
        final Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class, true);
        components.stream()
            .filter(ClassUtils::canBeInstantiated)
            .forEach(this::register);
    }

    private void registerAllDeclaredComponentFactories(final Reflections reflections) {
        Set<Class<? extends ComponentFactory>> factoryClasses = reflections.getSubTypesOf(ComponentFactory.class);
        for (Class<? extends ComponentFactory> factoryClass : factoryClasses) {
            if (ClassUtils.canBeInstantiated(factoryClass)) {
                final ComponentFactory<?> factory = ClassUtils.newInstance(factoryClass);
                reader.registerComponent(factory, registry);
            }
        }
    }

    private void register(final Class<?> type) {
        reader.registerComponent(type, registry);
    }
}
