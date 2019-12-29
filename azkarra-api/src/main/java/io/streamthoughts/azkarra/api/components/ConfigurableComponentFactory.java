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

import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Collection;

public interface ConfigurableComponentFactory {

    /**
     * Gets the internal {@link ComponentFactory}.
     *
     * @return  the {@link ComponentFactory} instance to be used.
     */
    ComponentFactory getComponentFactory();

    /**
     * Gets the configuration used by this component factory.
     *
     * @return  the {@link Conf}
     */
    Conf getConfiguration();

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param type      the component class.
     * @param <T>       the component-type.
     *
     * @return          the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    default <T> T  getComponent(final Class<T> type) {
        return getComponentFactory().getComponent(type, getConfiguration());
    }

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param type       the component class.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    default <T> T getComponent(final Class<T> type, final Qualifier<T> qualifier) {
        return getComponentFactory().getComponent(type, getConfiguration(), qualifier);
    }


    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param alias  the fully qualified class name or an alias of the component.
     * @param <T>    the component-type.
     *
     * @return       the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given class or alias..
     */
    default <T> T getComponent(final String alias) {
        return getComponentFactory().getComponent(alias, getConfiguration());
    }

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param alias      the fully qualified class name or an alias of the component.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given class or alias..
     */
    default <T> T getComponent(final String alias, final Qualifier<T> qualifier) {
        return getComponentFactory().getComponent(alias, getConfiguration(), qualifier);
    }

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param alias   the fully qualified class name or an alias of the component.
     * @param <T>     the component-type.
     *
     * @return        all instances of type {@link T}.
     */
    default <T> Collection<T> getAllComponents(final String alias) {
        return getComponentFactory().getAllComponents(alias, getConfiguration());
    }

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param alias      the fully qualified class name or an alias of the component.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           all instances of type {@link T}.
     */
    default <T> Collection<T> getAllComponents(final String alias, final Qualifier<T> qualifier) {
        return getComponentFactory().getAllComponents(alias, getConfiguration(), qualifier);
    }

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param type    the component class.
     * @param <T>     the component-type.
     *
     * @return        all instances of type {@link T}.
     */
    default <T> Collection<T> getAllComponents(final Class<T> type) {
        return getComponentFactory().getAllComponents(type, getConfiguration());
    }

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param type       the component class.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           all instances of type {@link T}.
     */
    default <T> Collection<T> getAllComponents(final Class<T> type, final Qualifier<T> qualifier) {
        return getComponentFactory().getAllComponents(type, getConfiguration(), qualifier);
    }
}
