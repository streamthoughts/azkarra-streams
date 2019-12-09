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
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentRegistry;
import io.streamthoughts.azkarra.runtime.components.DefaultProviderClassReader;
import io.streamthoughts.azkarra.streams.components.scantest.factory.TestComponentFactory;
import io.streamthoughts.azkarra.streams.components.scantest.test.TestAnnotatedComponent;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ComponentScannerTest {

    @TempDir
    static Path componentPath;

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredComponents() {
        ComponentClassReader reader = Mockito.mock(ComponentClassReader.class);
        ComponentScanner scanner = new ComponentScanner(reader, null);

        scanner.scan(TestAnnotatedComponent.class.getPackage());

        Mockito.verify(reader).registerComponent(
            Mockito.argThat(new ClassMatcher(TestAnnotatedComponent.class)),
            Mockito.any(ComponentRegistry.class),
            Mockito.any(ClassLoader.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredComponentFactory() {
        ComponentClassReader reader = Mockito.mock(ComponentClassReader.class);
        ComponentScanner scanner = new ComponentScanner(reader, null);

        scanner.scan(TestComponentFactory.class.getPackage());

        Mockito.verify(reader).registerComponent(
            Mockito.argThat(new ComponentFactoryMatcher(
                TestComponentFactory.DummyComponent.class,
                false)),
            Mockito.any(ComponentRegistry.class),
            Mockito.any(ClassLoader.class));
    }

    @Test
    public void shouldScanAndRegisterComponentsFromExternalPath() throws IOException {

        generateTopologyProviderClass("1.0", "component-version-1");

        generateTopologyProviderClass("2.0", "component-version-2");

        final DefaultProviderClassReader reader = new DefaultProviderClassReader();
        final DefaultComponentRegistry registry = new DefaultComponentRegistry();
        final ComponentScanner scanner = new ComponentScanner(reader, registry);

        scanner.scan(componentPath.toString());

        TopologyProvider pv1 = registry.getVersionedComponent(
                "test.ByteBuddyTopologyProvider", "1.0", Conf.empty());
        Assertions.assertEquals("1.0", pv1.version());

        TopologyProvider pv2 = registry.getVersionedComponent(
                "test.ByteBuddyTopologyProvider", "2.0", Conf.empty());
        Assertions.assertEquals("2.0", pv2.version());

    }

    private void generateTopologyProviderClass(final String version, final String path) throws IOException {
        new ByteBuddy()
            .subclass(TopologyProvider.class)
            .name("test.ByteBuddyTopologyProvider")
            .annotateType(AnnotationDescription.Builder.ofType(Component.class).build())
            .method(ElementMatchers.named("version"))
            .intercept(FixedValue.value(version))
            .make()
            .saveIn(new File(componentPath.toFile(), path));
    }

    public static class ClassMatcher extends ArgumentMatcher<Class> {

        private final Class<?> type;

        ClassMatcher(final Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean matches(Object o) {
            Class c = (Class) o;
            return o.equals(type);
        }
    }

    public static class ComponentFactoryMatcher extends ArgumentMatcher<ComponentFactory> {

        private final Class<?> type;
        private final boolean isSingleton;

        ComponentFactoryMatcher(final Class<?> type, final boolean isSingleton) {
            this.type = type;
            this.isSingleton = isSingleton;
        }

        @Override
        public boolean matches(Object o) {
            ComponentFactory c = (ComponentFactory) o;
            return c.getType().equals(type) && c.isSingleton() == isSingleton;
        }
    }
}