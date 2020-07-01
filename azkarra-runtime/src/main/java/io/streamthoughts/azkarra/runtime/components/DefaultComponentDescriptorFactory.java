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

import io.streamthoughts.azkarra.api.annotations.ConfValue;
import io.streamthoughts.azkarra.api.annotations.Order;
import io.streamthoughts.azkarra.api.components.ComponentAttribute;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorFactory;
import io.streamthoughts.azkarra.api.components.ComponentMetadata;
import io.streamthoughts.azkarra.api.components.ComponentNameGenerator;
import io.streamthoughts.azkarra.api.components.ComponentRegistrationException;
import io.streamthoughts.azkarra.api.components.Ordered;
import io.streamthoughts.azkarra.api.components.Versioned;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.util.AnnotationResolver;
import io.streamthoughts.azkarra.api.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

        final var classLoader = componentType.getClassLoader();
        final var version = getVersionFor(componentType, classLoader);

        final ComponentMetadata metadata = loadAnnotationComponentMetadata(componentType);

        var builder = new ComponentDescriptorBuilder<T>()
           .type(componentType)
           .supplier(componentSupplier)
           .metadata(metadata)
           .isSingleton(isSingleton)
           .order(extractComponentOrderFromMetadata(metadata))
           .configuration(extractComponentConfFromMetadata(metadata));

        if (version != null)
            builder.version(version);

        if (componentName != null)
            builder.name(componentName);
        else
            builder.name(componentNameGenerator.generate(builder));

        return builder.build();
    }

    private int extractComponentOrderFromMetadata(final ComponentMetadata metadata) {
        return metadata.attributesForName(attributeNameFor(Order.class))
            .stream()
            .findFirst()
            .map(attribute -> (int)attribute.value("value"))
            .orElse(Ordered.LOWEST_ORDER - 1);
    }

    private Conf extractComponentConfFromMetadata(final ComponentMetadata metadata) {
        return Conf.of(
            metadata.attributesForName(attributeNameFor(ConfValue.class))
                .stream()
                .collect(Collectors.toMap(
                    attr -> attr.stringValue("key"),
                    attr -> attr.stringValue("value")
                )
        ));
    }

    private <T> ComponentMetadata loadAnnotationComponentMetadata(final Class<T> componentType) {
        final var metadata = new ComponentMetadata();
        var annotations = AnnotationResolver.findAllAnnotations(componentType);
        for (var annotation : annotations) {
            Class<? extends Annotation> type = annotation.annotationType();
            var attribute = new ComponentAttribute(attributeNameFor(type));
            for (var method : type.getDeclaredMethods()) {
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
        return metadata;
    }

    private String attributeNameFor(final Class<? extends Annotation> annotationType) {
        return annotationType.getSimpleName().toLowerCase();
    }

    private static String getVersionFor(final Class<?> cls, final ClassLoader classLoader) {
        var saveLoader = ClassUtils.compareAndSwapLoaders(classLoader);
        try {
            String version = null;
            if (Versioned.class.isAssignableFrom(cls)) {
                version = ((Versioned)ClassUtils.newInstance(cls)).version();
                if (version == null)
                    throw new ComponentRegistrationException(
                        "Class '" + cls.getName() + "' must return a non-empty version.");
            }
            return version;
        } finally {
            ClassUtils.compareAndSwapLoaders(saveLoader);
        }
    }
}
