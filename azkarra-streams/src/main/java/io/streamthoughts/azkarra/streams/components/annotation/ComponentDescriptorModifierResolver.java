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
package io.streamthoughts.azkarra.streams.components.annotation;

import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;
import io.streamthoughts.azkarra.api.util.AnnotationResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * This interface can be used to resolve a {@link ComponentDescriptorModifier} to be used for registering a component.
 */
public interface ComponentDescriptorModifierResolver {

    /**
     * Resolves the {@link ComponentDescriptorModifier} for the given component type.
     *
     * @param clazz the {@link Class} that defines the component.
     *
     * @return      an optional {@link ComponentDescriptorModifier}.
     */
    Optional<ComponentDescriptorModifier> resolves(final Class<?> clazz);

    /**
     * Resolves the {@link ComponentDescriptorModifier} for the given component type.
     *
     * @param method the {@link Method} that defines the component.
     * @return       an optional {@link ComponentDescriptorModifier}.
     */
    Optional<ComponentDescriptorModifier> resolves(final Method method);

    /**
     * Returns the given {@link ComponentDescriptorModifier} only if the component is
     * annotated with the given component.
     *
     * @param annotationType    the {@link Annotation} type to check.
     * @param modifier          the {@link ComponentDescriptorModifier} to add.
     */
    static ComponentDescriptorModifierResolver onAnnotationExists(final Class<? extends Annotation> annotationType,
                                                                  final ComponentDescriptorModifier modifier) {
        return new ApplyModifierOnAnnotationExist(annotationType, modifier);
    }

    /**
     * Returns the given {@link ComponentDescriptorModifier} only if the component is
     * annotated with the given component.
     *
     * @param annotationType    the {@link Annotation} type to check.
     * @param modifier          the {@link ComponentDescriptorModifier} to add.
     */
    static <T extends Annotation> ComponentDescriptorModifierResolver onAnnotations(
                                                     final Class<T> annotationType,
                                                     final Function<List<T>, ComponentDescriptorModifier> modifier) {
        return new ApplyModifierOnAnnotations<>(annotationType, modifier);
    }

    /**
     * Returns the given {@link ComponentDescriptorModifier} only if the component is
     * annotated with the given component.
     *
     * @param annotationType    the {@link Annotation} type to check.
     * @param modifier          the {@link ComponentDescriptorModifier} to add.
     */
    static <T extends Annotation> ComponentDescriptorModifierResolver onSingleAnnotation(
            final Class<T> annotationType,
            final Function<T, ComponentDescriptorModifier> modifier) {
        return new ApplyModifierOnAnnotations<>(annotationType, annotations -> modifier.apply(annotations.get(0)));
    }

    class ApplyModifierOnAnnotationExist implements ComponentDescriptorModifierResolver {

        private final Class<? extends Annotation>  annotation;

        private final ComponentDescriptorModifier modifier;

        ApplyModifierOnAnnotationExist(final Class<? extends Annotation> annotation,
                                       final ComponentDescriptorModifier modifier) {
            this.annotation = annotation;
            this.modifier = modifier;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ComponentDescriptorModifier> resolves(final Class<?> clazz) {
            return AnnotationResolver.isAnnotatedWith(clazz, annotation) ? Optional.of(modifier) : Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ComponentDescriptorModifier> resolves(final Method method) {
            return AnnotationResolver.isAnnotatedWith(method, annotation) ? Optional.of(modifier) : Optional.empty();
        }
    }

    class ApplyModifierOnAnnotations<T extends Annotation> implements ComponentDescriptorModifierResolver {

        private final Class<T>  annotation;
        private final Function<List<T>, ComponentDescriptorModifier> modifier;

        ApplyModifierOnAnnotations(final Class<T> annotation,
                                   final Function<List<T>, ComponentDescriptorModifier> modifier) {
            this.annotation = annotation;
            this.modifier = modifier;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ComponentDescriptorModifier> resolves(final Class<?> clazz) {
            var annotations = AnnotationResolver.findAllAnnotationsByType(clazz, annotation);
            return !annotations.isEmpty() ? Optional.of(modifier.apply(annotations)) : Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ComponentDescriptorModifier> resolves(final Method method) {
            var annotations = AnnotationResolver.findAllAnnotationsByType(method, annotation);
            return !annotations.isEmpty() ? Optional.of(modifier.apply(annotations)) : Optional.empty();
        }
    }
}
