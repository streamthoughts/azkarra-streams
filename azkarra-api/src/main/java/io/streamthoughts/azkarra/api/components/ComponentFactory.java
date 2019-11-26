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
package io.streamthoughts.azkarra.api.components;

import java.util.Objects;

/**
 * The {@link ComponentFactory} interface is used for creating new component instance.
 *
 * @see ComponentRegistry
 * @see ComponentDescriptor
 *
 * @param <T>   the component-type.
 */
public interface ComponentFactory<T> {

    static <T> ComponentFactory<T> singletonOf(final T instance) {
        return new SimpleFactory<>(instance, true);
    }

    static <T> ComponentFactory<T> prototypeOf(final T instance) {
        return new SimpleFactory<>(instance, false);
    }

    /**
     * Creates a new instance for the specified type.
     *
     * @return  the new instance of type {@link T}.
     */
    T make();

    /**
     * Returns the type of the component created by this {@link ComponentFactory}.
     *
     * @return  a {@link Class} instance.
     */
    Class<T> getType();

    /**
     * Returns the if the instance created from this {@link ComponentFactory} must be shared across the application.
     *
     * @return  {@code true} if the instance is shared, {@code false} otherwise.
     */
    default boolean isSingleton() {
        return false;
    }

    final class SimpleFactory<T> implements ComponentFactory<T> {

        private final T instance;
        private final Class<T> type;
        private final boolean isSingleton;

        /**
         * Creates a new {@link SimpleFactory} instance.
         *
         * @param instance     the instance of type {@link T}.
         * @param isSingleton  is the component shared across the application.
         */
        @SuppressWarnings("unchecked")
        SimpleFactory(final T instance, final boolean isSingleton) {
            Objects.requireNonNull(instance, "instance cannot be null");
            this.instance = instance;
            this.type = (Class<T>)instance.getClass();
            this.isSingleton = isSingleton;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T make() {
            return instance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<T> getType() {
            return type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSingleton() {
            return isSingleton;
        }
    }
}
