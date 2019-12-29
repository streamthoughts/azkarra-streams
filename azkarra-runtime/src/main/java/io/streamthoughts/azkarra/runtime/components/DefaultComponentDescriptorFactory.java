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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.components.ComponentAttribute;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.components.ComponentMetadata;
import io.streamthoughts.azkarra.api.components.ComponentNameGenerator;
import io.streamthoughts.azkarra.api.components.ComponentRegistrationException;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import io.streamthoughts.azkarra.api.components.Versioned;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import io.streamthoughts.azkarra.api.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class DefaultComponentDescriptorFactory implements ComponentDescriptorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultComponentDescriptorFactory.class);

    private ComponentNameGenerator componentNameGenerator;

    /**
     * Creates a new {@link DefaultComponentDescriptorFactory} instance.
     */
    public DefaultComponentDescriptorFactory() {
        componentNameGenerator = ComponentNameGenerator.DEFAULT;
    }

    /**
     * Sets the {@link ComponentNameGenerator} used for generate a component name if no one is provided.
     *
     * @param componentNameGenerator the {@link ComponentNameGenerator}.
     */
    public void setComponentNameGenerator(final ComponentNameGenerator componentNameGenerator) {
        this.componentNameGenerator = Objects.requireNonNull(
            componentNameGenerator,
            "componentNameGenerator can't be null"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ComponentDescriptor<T> make(final String componentName,
                                           final Class<T> componentType,
                                           final Supplier<T> componentSupplier,
                                           final boolean isSingleton) {

        final ClassLoader classLoader = componentType.getClassLoader();
        final String version = getVersionFor(componentType, classLoader);


        final ComponentMetadata metadata = new ComponentMetadata();

        ComponentDescriptorBuilder<T> builder = new ComponentDescriptorBuilder<T>()
           .type(componentType)
           .supplier(componentSupplier)
           .metadata(metadata)
           .isSingleton(isSingleton);

        if (version != null)
            builder.version(version);

        List<Annotation> allDeclaredAnnotations = ClassUtils.getAllDeclaredAnnotations(componentType);

        for (Annotation annotation : allDeclaredAnnotations) {
            Class<? extends Annotation> type = annotation.annotationType();
            ComponentAttribute attribute = new ComponentAttribute(type.getSimpleName().toLowerCase());
            for (Method method : type.getDeclaredMethods()) {
                try {
                    Object defaultValue = method.getDefaultValue();
                    Object value = method.invoke(annotation);
                    attribute.add(method.getName(), value, defaultValue);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error("Error while scanning component annotations", e);
                }
            }
            metadata.addAttribute(attribute);
        }

        if (componentName != null)
            builder.name(componentName);
        else
            builder.name(componentNameGenerator.generate(builder));

        return builder.build();
    }


    @SuppressWarnings("unchecked")
    private static String getVersionFor(final Class<?> cls, final ClassLoader classLoader) {
        ClassLoader saveLoader = ClassUtils.compareAndSwapLoaders(classLoader);
        try {
            String version = null;
            if (Versioned.class.isAssignableFrom(cls)) {
                version = ClassUtils.newInstance((Class<Versioned>) cls).version();
                if (version == null)
                    throw new ComponentRegistrationException(
                        "Class '" + cls.getName() + "' must return a non-empty version.");
            }
            return version;
        } finally {
            ClassUtils.compareAndSwapLoaders(saveLoader);
        }
    }

    private static class ComponentDescriptorBuilder<T> implements ComponentDescriptor<T> {

        private String name;
        private ComponentMetadata metadata;
        private Class<T> type;
        private ClassLoader classLoader;
        private String version;
        private Supplier<T> supplier;
        private boolean isSingleton;
        private Set<String> aliases = new HashSet<>();

        @Override
        public String name() {
            return name;
        }

        ComponentDescriptorBuilder<T> name(final String name) {
            this.name = name;
            return this;
        }

        @Override
        public ComponentMetadata metadata() {
            return metadata;
        }

        ComponentDescriptorBuilder<T> metadata(final ComponentMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        @Override
        public ClassLoader classLoader() {
            return classLoader;
        }

        ComponentDescriptorBuilder<T> classLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        @Override
        public void addAliases(final Set<String> aliases) {
            this.aliases.addAll(aliases);
        }

        @Override
        public Set<String> aliases() {
            return aliases;
        }

        @Override
        public Version version() {
            return version != null ? Version.parse(version) : null;
        }

        ComponentDescriptorBuilder<T> version(final String version) {
            this.version = version;
            return this;
        }

        @Override
        public Supplier<T> supplier() {
            return supplier;
        }

        ComponentDescriptorBuilder<T> supplier(final Supplier<T> supplier) {
            this.supplier = supplier;
            return this;
        }

        @Override
        public Class<T> type() {
            return type;
        }

        ComponentDescriptorBuilder<T> type(final Class<T> type) {
            this.type = type;
            return this;
        }

        @Override
        public boolean isSingleton() {
            return isSingleton;
        }

        ComponentDescriptorBuilder<T> isSingleton(final boolean isSingleton) {
            this.isSingleton = isSingleton;
            return this;
        }

        @Override
        public int compareTo(final ComponentDescriptor<T> o) {
            throw new UnsupportedOperationException();
        }

        public ComponentDescriptor<T> build() {
            SimpleComponentDescriptor<T> descriptor = new SimpleComponentDescriptor<>(
                name,
                type,
                classLoader,
                supplier,
                version,
                isSingleton
            );
            descriptor.metadata(metadata);
            descriptor.addAliases(aliases);
            return descriptor;
        }
    }

}
