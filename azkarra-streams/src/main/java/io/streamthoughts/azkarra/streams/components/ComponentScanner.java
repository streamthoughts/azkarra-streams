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
import io.streamthoughts.azkarra.streams.components.isolation.ComponentClassLoader;
import io.streamthoughts.azkarra.streams.components.isolation.ComponentResolver;
import io.streamthoughts.azkarra.streams.components.isolation.ExternalComponent;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link ComponentScanner} class can be used used to scan the classpath for automatically
 * registering declared classes annotated with {@link Component} and {@link ComponentFactory} classes.
 */
public class ComponentScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentScanner.class);

    private final ComponentClassReader reader;
    private final ComponentRegistry registry;

    private FilterBuilder filterBuilder;

    /**
     * Creates a new {@link ComponentScanner} instance.
     *
     * @param reader    the {@link DefaultProviderClassReader} used to register providers.
     * @param registry  the {@link ComponentRegistry} used to register providers.
     */
    public ComponentScanner(final ComponentClassReader reader,
                            final ComponentRegistry registry) {
        Objects.requireNonNull(reader, "reader cannot be null");
        Objects.requireNonNull(reader, "registry cannot be null");
        this.reader = reader;
        this.registry = registry;
        this.filterBuilder = new FilterBuilder();
    }

    /**
     * Scans external component for the specified paths.
     *
     * @param componentPaths   the comma-separated list of top-level components directories.
     */
    public void scan(final String componentPaths) {
       final List<String> paths = Arrays
           .stream(componentPaths.split(","))
           .map(String::trim)
           .collect(Collectors.toList());
        scan(paths);
    }

    /**
     * Scans external component for the specified paths.
     *
     * @param componentPaths   the list of top-level components directories.
     */
    public void scan(final List<String> componentPaths) {

        for (final String path : componentPaths) {
            try {
                final Path componentPath = Paths.get(path).toAbsolutePath();
                scanComponentPaths(componentPath);
            } catch (InvalidPathException e) {
                LOG.error("Ignoring top-level component location '{}', invalid path.", path);
            }
        }
    }

    /**
     * Scans the specified top-level component directory for components.
     *
     * @param componentPaths   the absolute path to a top-level component directory.
     */
    private void scanComponentPaths(final Path componentPaths) {

        ComponentResolver resolver = new ComponentResolver(componentPaths);
        for (ExternalComponent component : resolver.resolves()) {
            LOG.info("Loading components from path : {}", component.location());
            final ComponentClassLoader classLoader = ComponentClassLoader.newClassLoader(
                component.location(),
                component.resources(),
                ComponentScanner.class.getClassLoader()
            );
            LOG.info("Initialized new ClassLoader: {}", classLoader);
            scanUrlsForComponents(component.resources(), classLoader);
        }
    }

    public void scan(final Package source) {
        Objects.requireNonNull(source);
        final URL[] urls = ClasspathHelper.forPackage(source.getName()).toArray(new URL[0]);
        filterBuilder.includePackage(source.getName());
        scanUrlsForComponents(urls, ComponentScanner.class.getClassLoader());
    }

    private void scanUrlsForComponents(final URL[] urls,
                                       final ClassLoader classLoader) {
        LOG.info("Scanning components from paths : {}",
            Arrays.stream(urls).map(URL::getPath).collect(Collectors.joining("", "\n\t", "")));

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setClassLoaders(new ClassLoader[]{classLoader});
        builder.addUrls(urls);
        builder.filterInputsBy(filterBuilder);
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        builder.useParallelExecutor();

        Reflections reflections = new SafeReflections(builder);

        registerAllDeclaredComponentFactories(reflections, classLoader);
        registerAllDeclaredComponents(reflections, classLoader);
    }

    private void registerAllDeclaredComponents(final Reflections reflections,
                                               final ClassLoader classLoader) {
        final Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class, true);
        components.stream()
            .filter(ClassUtils::canBeInstantiated)
            .forEach(type ->  reader.registerComponent(type, registry, classLoader));
    }

    private void registerAllDeclaredComponentFactories(final Reflections reflections,
                                                       final ClassLoader classLoader) {
        Set<Class<? extends ComponentFactory>> factoryClasses = reflections.getSubTypesOf(ComponentFactory.class);
        for (Class<? extends ComponentFactory> factoryClass : factoryClasses) {
            if (ClassUtils.canBeInstantiated(factoryClass)) {
                final ComponentFactory<?> factory = ClassUtils.newInstance(factoryClass);
                reader.registerComponent(factory, registry, classLoader);
            }
        }
    }

    // The Reflections class may throw a ReflectionsException when parallel executor
    // is used and an unsupported URL type is scanned.
    private static class SafeReflections extends Reflections {

        SafeReflections(final Configuration configuration) {
            super(configuration);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void scan(final URL url) {
            try {
                super.scan(url);
            } catch (ReflectionsException e) {
                final Logger log = Reflections.log;
                if (log != null && log.isWarnEnabled()) {
                    log.warn("could not create Vfs.Dir from url, " +
                        "no matching UrlType was found. Ignoring the exception and continuing", e);
                }
            }
        }
    }
}
