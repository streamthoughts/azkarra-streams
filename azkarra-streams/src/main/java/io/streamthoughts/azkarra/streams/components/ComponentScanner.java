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
import io.streamthoughts.azkarra.api.annotations.ConditionalOn;
import io.streamthoughts.azkarra.api.annotations.Eager;
import io.streamthoughts.azkarra.api.annotations.Factory;
import io.streamthoughts.azkarra.api.annotations.Order;
import io.streamthoughts.azkarra.api.annotations.Primary;
import io.streamthoughts.azkarra.api.annotations.Secondary;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.api.components.condition.Conditions;
import io.streamthoughts.azkarra.api.errors.AzkarraException;
import io.streamthoughts.azkarra.api.util.AnnotationResolver;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import io.streamthoughts.azkarra.runtime.components.BasicComponentFactory;
import io.streamthoughts.azkarra.streams.components.isolation.ComponentClassLoader;
import io.streamthoughts.azkarra.streams.components.isolation.ComponentResolver;
import io.streamthoughts.azkarra.streams.components.isolation.ExternalComponent;
import org.reflections.Configuration;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.asEager;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.asPrimary;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.asSecondary;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.withConditions;
import static io.streamthoughts.azkarra.runtime.components.ComponentDescriptorModifiers.withOrder;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;

/**
 * The {@link ComponentScanner} class can be used used to scan the classpath for automatically
 * registering declared classes annotated with {@link Component} and {@link ComponentFactory} classes.
 */
public class ComponentScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentScanner.class);

    private static final Predicate<Method> GET_METHOD = ReflectionUtils.withName("get")::apply;

    private static final FilterBuilder DEFAULT_FILTER_BY = new FilterBuilder();

    private final ComponentRegistry registry;

    /**
     * Creates a new {@link ComponentScanner} instance.
     *
     * @param registry  the {@link ComponentRegistry} used to register providers.
     */
    public ComponentScanner(final ComponentRegistry registry) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.registry = registry;
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
            scanUrlsForComponents(component.resources(), classLoader, DEFAULT_FILTER_BY);
        }
    }

    /**
     * Scans the specified package for components.
     *
     * @param source    the {@link Package} to be scanned; must not be {@code null}.
     */
    public void scanForPackage(final Package source) {
        Objects.requireNonNull(source, "source package cannot be null");
        scanForPackage(source.getName());
    }

    /**
     * Scans the specified package for components.
     *
     * @param source    the package to be scanned; must not be {@code null}.
     */
    public void scanForPackage(final String source) {
        Objects.requireNonNull(source, "source package cannot be null");
        LOG.info("Looking for paths to scan from source package {}", source);
        final URL[] urls = ClasspathHelper.forPackage(source).toArray(new URL[0]);
        final FilterBuilder filterBy =  new FilterBuilder().includePackage(source);
        scanUrlsForComponents(urls, ComponentScanner.class.getClassLoader(), filterBy);
    }

    private void scanUrlsForComponents(final URL[] urls,
                                       final ClassLoader classLoader,
                                       final com.google.common.base.Predicate<String> filterBy) {
        LOG.info("Scanning components from paths : {}",
            Arrays.stream(urls).map(URL::getPath).collect(Collectors.joining("\n\t", "\n\t", "")));

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setClassLoaders(new ClassLoader[]{classLoader});
        builder.addUrls(urls);
        builder.filterInputsBy(filterBy);
        builder.setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        builder.useParallelExecutor();

        Reflections reflections = new SafeReflections(builder);

        registerClassesAnnotatedComponent(reflections, classLoader);
        registerClassesAnnotatedFactory(reflections, classLoader);
    }

    private void registerClassesAnnotatedFactory(final Reflections reflections,
                                                 final ClassLoader classLoader) {
        final Set<Class<?>> factoryClasses = reflections.getTypesAnnotatedWith(Factory.class, true);
        for (Class<?> factoryClass : factoryClasses) {
            if (ClassUtils.canBeInstantiated(factoryClass)) {
                Set<Method> components = getAllMethods(factoryClass, withAnnotation(Component.class));
                for (Method method : components) {
                    registerComponentMethod(factoryClass, method, classLoader);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerClassesAnnotatedComponent(final Reflections reflections,
                                                   final ClassLoader classLoader) {
        final Set<Class<?>> components = reflections.getTypesAnnotatedWith(Component.class, true);

        for (Class<?> component : components) {
            if (ClassUtils.canBeInstantiated(component)) {
                registerComponentClass((Class<Object>)component, classLoader);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerComponentMethod(final Class<?> factoryClass,
                                         final Method method,
                                         final ClassLoader classLoader) {
        final Object target = ClassUtils.newInstance(factoryClass, classLoader);
        final Class<Object> componentClass = (Class<Object>) method.getReturnType();
        final ReflectMethodComponentSupplier supplier = new ReflectMethodComponentSupplier(target, method);

        final String componentName = getNamedQualifierOrElse(method, method.getName());

        List<ComponentDescriptorModifier> modifiers = new ArrayList<>();

        mayAddModifierForOrder(method, modifiers);
        mayAddModifierForPrimary(method, modifiers);
        mayAddModifierForSecondary(method, modifiers);
        mayAddModifierForCondition(method, modifiers);
        mayAddModifierForEagerlyInitialization(method, modifiers);

        final ComponentDescriptorModifier[] objects = modifiers.toArray(new ComponentDescriptorModifier[]{});
        registerComponent(componentName, componentClass, supplier, isSingleton(method), objects);
    }

    @SuppressWarnings("unchecked")
    private void registerComponentClass(final Class<Object> cls, final ClassLoader classLoader) {
        final Supplier<Object> supplier;
        final Class<Object> type;
        if (isSupplier(cls)) {
            supplier = (Supplier<Object>) ClassUtils.newInstance(cls, classLoader);
            type = resolveSupplierReturnType(cls);
            if (type == null)
                throw new AzkarraException("Unexpected error while scanning component. " +
                    "Cannot resolve return type from supplier: " + cls.getName());
        } else {
            supplier = new BasicComponentFactory<>(cls, classLoader);
            type = cls;
        }

        List<ComponentDescriptorModifier> modifiers = new ArrayList<>();
        mayAddModifierForOrder(cls, modifiers);
        mayAddModifierForPrimary(cls, modifiers);
        mayAddModifierForSecondary(cls, modifiers);
        mayAddModifierForCondition(cls, modifiers);
        mayAddModifierForEagerlyInitialization(cls, modifiers);

        final ComponentDescriptorModifier[] arrayModifiers = modifiers.toArray(new ComponentDescriptorModifier[]{});

        final String componentName = getNamedQualifierOrNull(cls);
        registerComponent(componentName, type, supplier, isSingleton(cls), arrayModifiers);
    }

    private void registerComponent(final String componentName,
                                   final Class<Object> type,
                                   final Supplier<Object> supplier,
                                   final boolean isSingleton,
                                   final ComponentDescriptorModifier... modifiers) {
        if (isSingleton)
            registry.registerSingleton(componentName, type, supplier, modifiers);
        else
            registry.registerComponent(componentName, type, supplier, modifiers);
    }

    @SuppressWarnings("unchecked")
    private static Class<Object> resolveSupplierReturnType(final Class<Object> cls) {
        Class<Object> type = null;
        Set<Method> methods = ReflectionUtils.getMethods(cls, GET_METHOD::test);
        for (Method m : methods) {
            if (!m.isBridge())
                type = (Class<Object>) m.getReturnType();
        }
        return type;
    }

    private static boolean isSupplier(final Class<?> componentClass) {
        return Supplier.class.isAssignableFrom(componentClass);
    }

    private static boolean isSingleton(final Class<?> componentClass) {
        return AnnotationResolver.isAnnotatedWith(componentClass, Singleton.class);
    }

    private static boolean isSingleton(final Method method) {
        return AnnotationResolver.isAnnotatedWith(method, Singleton.class);
    }

    private static String getNamedQualifierOrNull(final Class<?> componentClass) {
        List<Named> annotations = AnnotationResolver.findAllAnnotationsByType(componentClass, Named.class);
        return annotations.isEmpty() ? null : annotations.get(0).value();
    }

    private static String getNamedQualifierOrElse(final Method componentMethod, final String defaultName) {
        Named annotation = componentMethod.getDeclaredAnnotation(Named.class);
        return annotation == null ? defaultName : annotation.value();
    }

    private static void mayAddModifierForOrder(final Method componentMethod,
                                               final List<ComponentDescriptorModifier> modifiers) {
        Order annotation = componentMethod.getDeclaredAnnotation(Order.class);
        if (annotation != null)
            modifiers.add(withOrder(annotation.value()));
    }

    private static void mayAddModifierForOrder(final Class<?> cls,
                                               final List<ComponentDescriptorModifier> modifiers) {
        List<Order> annotations = AnnotationResolver.findAllAnnotationsByType(cls, Order.class);
        if (!annotations.isEmpty())
            modifiers.add(withOrder(annotations.get(0).value()));
    }

    private static void mayAddModifierForPrimary(final Class<?> componentClass,
                                                 final List<ComponentDescriptorModifier> modifiers) {
        if (AnnotationResolver.isAnnotatedWith(componentClass, Primary.class))
            modifiers.add(asPrimary());
    }

    private static void mayAddModifierForPrimary(final Method method,
                                                 final List<ComponentDescriptorModifier> modifiers) {
        if (AnnotationResolver.isAnnotatedWith(method, Primary.class))
            modifiers.add(asPrimary());
    }

    private static void mayAddModifierForSecondary(final Method method,
                                                   final List<ComponentDescriptorModifier> modifiers) {
        if (AnnotationResolver.isAnnotatedWith(method, Secondary.class))
            modifiers.add(asSecondary());
    }

    private static void mayAddModifierForSecondary(final Class<?> componentClass,
                                                   final List<ComponentDescriptorModifier> modifiers) {
        if (AnnotationResolver.isAnnotatedWith(componentClass, Secondary.class))
            modifiers.add(asSecondary());
    }

    private static void mayAddModifierForCondition(final Method componentMethod,
                                               final List<ComponentDescriptorModifier> modifiers) {
        mayAddModifiersForConditions(
            modifiers,
            AnnotationResolver.findAllAnnotationsByType(componentMethod, ConditionalOn.class)
        );
    }

    private static void mayAddModifierForCondition(final Class<?> componentClass,
                                                   final List<ComponentDescriptorModifier> modifiers) {
        mayAddModifiersForConditions(
            modifiers,
            AnnotationResolver.findAllAnnotationsByType(componentClass, ConditionalOn.class)
        );
    }

    private static void mayAddModifiersForConditions(final List<ComponentDescriptorModifier> modifiers,
                                                     final List<ConditionalOn> annotations) {
        var conditions = Conditions.buildConditionsForAnnotation(annotations);
        if (!conditions.isEmpty()) {
            modifiers.add(withConditions(conditions));
        }
    }

    private static void mayAddModifierForEagerlyInitialization(final Method method,
                                                               final List<ComponentDescriptorModifier> modifiers) {
        if (AnnotationResolver.isAnnotatedWith(method, Eager.class))
            modifiers.add(asEager());
    }

    private static void mayAddModifierForEagerlyInitialization(final Class<?> componentClass,
                                                               final List<ComponentDescriptorModifier> modifiers) {
        if (AnnotationResolver.isAnnotatedWith(componentClass, Eager.class))
            modifiers.add(asEager());
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
