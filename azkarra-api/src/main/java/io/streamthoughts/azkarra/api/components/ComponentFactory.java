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
package io.streamthoughts.azkarra.api.components;

import io.streamthoughts.azkarra.api.components.condition.Condition;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public interface ComponentFactory extends
        ComponentRegistry, ComponentDescriptorRegistry, ConditionalDescriptorRegistry, Closeable {

    /**
     * Checks if at least one component is registered for the given type.
     *
     * @param type   the fully qualified class name or an alias of the component.
     *
     * @return       {@code true} if a provider exist, {@code false} otherwise.
     */
    boolean containsComponent(final String type);

    /**
     * Checks if at least one component is registered for the given type and qualifier.
     *
     * @param type       the component type.
     * @param qualifier  the options to qualified the component.
     * @return           {@code true} if a provider exist, {@code false} otherwise.
     */
    <T> boolean containsComponent(final String type, final Qualifier<T> qualifier);

    /**
     * Checks if at least one component is registered for the given type.
     *
     * @param type  the component type.
     * @return      {@code true} if a provider exist, {@code false} otherwise.
     */
    <T> boolean containsComponent(final Class<T> type) ;

    /**
     * Checks if at least one component is registered for the given type and qualifier.
     *
     * @param type       the component type.
     * @param qualifier  the options to qualified the component.
     * @return           {@code true} if a provider exist, {@code false} otherwise.
     */
    <T> boolean containsComponent(final Class<T> type, final Qualifier<T> qualifier);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param type      the component class.
     * @param conf      the configuration used if the component implement {@link Configurable}.
     * @param <T>       the component-type.
     *
     * @return          the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    <T> T getComponent(final Class<T> type, final Conf conf);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param type       the component class.
     * @param conf       the configuration used if the component implement {@link Configurable}.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return          the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    <T> T getComponent(final Class<T> type, final Conf conf, final Qualifier<T> qualifier);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * @param type       the component class.
     * @param qualifier  the options used to qualified the component.
     * @param <T>        the component-type.
     *
     * @return          the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given type.
     */
    <T> GettableComponent<T> getComponentProvider(final Class<T> type,
                                                  final Qualifier<T> qualifier);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * If the object, returned from that method, implements the {@link Configurable} interface, then the
     * factory will invoke the method {@link Configurable#configure(Conf)} with the given {@link Conf}.
     * For a shared object, the method will be invoked only the first time it is returned.
     *
     * @param type   the fully qualified class name or an alias of the component.
     * @param conf   the configuration used if the component implement {@link Configurable}.
     * @param <T>    the component-type.
     *
     * @return       the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given class or alias..
     */
    <T> T getComponent(final String type, final Conf conf);

    /**
     * Gets an instance, which may be shared or independent, for the specified type.
     *
     * If the object, returned from that method, implements the {@link Configurable} interface, then the
     * factory will invoke the method {@link Configurable#configure(Conf)} with the given {@link Conf}.
     * For a shared object, the method will be invoked only the first time it is returned.
     *
     * @param type       the fully qualified class name or an alias of the component.
     * @param conf       the configuration used if the component implement {@link Configurable}.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           the instance of type {@link T}.
     *
     * @throws NoUniqueComponentException   if more than one component is registered for the given type.
     * @throws NoSuchComponentException     if no component is registered for the given class or alias..
     */
    <T> T getComponent(final String type, final Conf conf, final Qualifier<T> qualifier);

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * If one of the objects, returned from that method, implements the {@link Configurable} interface,
     * then the factory will invoke the method {@link Configurable#configure(Conf)} with the given {@link Conf}.
     * For a shared object, the method will be invoked only the first time it is returned.
     *
     * @param type   the fully qualified class name or an alias of the component.
     * @param conf   the configuration used if the component implement {@link Configurable}.
     * @param <T>    the component-type.
     *
     * @return        all instances of type {@link T}.
     */
    <T> Collection<T> getAllComponents(final String type, final Conf conf);

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * If one of the objects, returned from that method, implements the {@link Configurable} interface,
     * then the factory will invoke the method {@link Configurable#configure(Conf)} with the given {@link Conf}.
     * For a shared object, the method will be invoked only the first time it is returned.
     *
     * @param type       the fully qualified class name or an alias of the component.
     * @param conf       the configuration used if the component implement {@link Configurable}.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           all instances of type {@link T}.
     */
    <T> Collection<T> getAllComponents(final String type, final Conf conf, final Qualifier<T> qualifier);

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * If one of the objects, returned from that method, implements the {@link Configurable} interface,
     * then the factory will invoke the method {@link Configurable#configure(Conf)} with the given {@link Conf}.
     * For a shared object, the method will be invoked only the first time it is returned.
     *
     * @param type    the component class.
     * @param conf    the configuration used if the component implement {@link Configurable}.
     * @param <T>     the component-type.
     *
     * @return        all instances of type {@link T}.
     */
    <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf);

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * If one of the objects, returned from that method, implements the {@link Configurable} interface,
     * then the factory will invoke the method {@link Configurable#configure(Conf)} with the given {@link Conf}.
     * For a shared object, the method will be invoked only the first time it is returned.
     *
     * @param type       the component class.
     * @param conf       the configuration used if the component implement {@link Configurable}.
     * @param conf       the {@link Conf} that may be used to match components {@link Condition}.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           all instances of type {@link T}.
     */
    <T> Collection<T> getAllComponents(final Class<T> type, final Conf conf, final Qualifier<T> qualifier);

    /**
     * Gets all instances, which may be shared or independent, for the specified type.
     *
     * @param type       the component class.
     * @param qualifier  the options used to qualify the component.
     * @param <T>        the component-type.
     *
     * @return           all instances of type {@link T}.
     */
    <T> Collection<GettableComponent<T>> getAllComponentProviders(final Class<T> type,
                                                                  final Qualifier<T> qualifier);

    /**
     * Initialize the component factory for the given configuration.
     *
     * @param conf  the configuration.
     */
    void init(final Conf conf);

    @Override
    void close() throws IOException;

    /**
     * @return the set of known {@link ClassLoader}.
     */
    Set<ClassLoader> getAllClassLoaders();

    /**
     * Loads all services for the given type using the standard Java {@link java.util.ServiceLoader} mechanism.
     *
     * @param type  the service Class type.
     * @param <T>   the service type.
     * @return      the services implementing the given type.
     */
    default <T> List<T> loadAllServices(final Class<T> type) {
        final List<T> loaded = new LinkedList<>();
        final Set<Class<? extends T>> types = new HashSet<>();
        for (ClassLoader cl : getAllClassLoaders()) {
            ServiceLoader<T> serviceLoader = ServiceLoader.load(type, cl);
            var providers = serviceLoader.stream().collect(Collectors.toList());
            for (ServiceLoader.Provider<T> provider : providers) {
                if (!types.contains(provider.type())) {
                    types.add(provider.type());
                    loaded.add(provider.get());
                }
            }
        }
        return loaded;
    }
}
