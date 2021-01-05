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
package io.streamthoughts.azkarra.streams.components;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentDescriptorModifier;
import io.streamthoughts.azkarra.api.components.ComponentScanner;
import io.streamthoughts.azkarra.api.components.qualifier.Qualifiers;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.util.Version;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentDescriptorFactory;
import io.streamthoughts.azkarra.runtime.components.DefaultComponentFactory;
import io.streamthoughts.azkarra.streams.MockTopologyProvider;
import io.streamthoughts.azkarra.streams.components.scan.component.TestAnnotatedComponent;
import io.streamthoughts.azkarra.streams.components.scan.factory.TestAnnotatedFactory;
import io.streamthoughts.azkarra.streams.components.scan.secondary.TestSecondaryComponent;
import io.streamthoughts.azkarra.streams.components.scan.supplier.TestSupplier;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReflectiveComponentScannerTest {

    @TempDir
    static Path COMPONENT_PATH;

    private DefaultComponentFactory factory;
    private ComponentScanner scanner;

    @BeforeEach
    public void setUp() {
        factory = Mockito.spy(new DefaultComponentFactory(new DefaultComponentDescriptorFactory()));
        scanner = new ReflectiveComponentScanner(factory);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredComponents() {
        scanner.scanForPackage(TestAnnotatedComponent.class.getPackage());

        Mockito.verify(factory).registerComponent(
            Matchers.isNull(String.class),
            Mockito.argThat(new ClassMatcher(TestAnnotatedComponent.class)),
            Mockito.any(Supplier.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredSupplier() {
        scanner.scanForPackage(TestSupplier.class.getPackage());
        Mockito.verify(factory).registerComponent(
                Matchers.isNull(String.class),
                Mockito.argThat(new ClassMatcher(MockTopologyProvider.class)),
                Mockito.any(Supplier.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredAllComponentFactory() {
        scanner.scanForPackage(TestAnnotatedFactory.class.getPackage());

        Mockito.verify(factory, Mockito.times(2)).registerComponent(
            Mockito.anyString(),
            Mockito.argThat(new ClassMatcher(TestAnnotatedFactory.DummyComponent.class)),
            Mockito.any(Supplier.class));

        Mockito.verify(factory, Mockito.times(1)).registerSingleton(
            Mockito.anyString(),
            Mockito.argThat(new ClassMatcher(TestAnnotatedFactory.DummyComponent.class)),
            Mockito.any(Supplier.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredComponentFactory() {
        scanner.scanForPackage(TestAnnotatedFactory.class.getPackage());

        Mockito.verify(factory).registerComponent(
            Mockito.eq("testComponent"),
            Mockito.argThat(new ClassMatcher(TestAnnotatedFactory.DummyComponent.class)),
            Mockito.any(Supplier.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredSingletonFactory() {
        scanner.scanForPackage(TestAnnotatedFactory.class.getPackage());

        Mockito.verify(factory).registerSingleton(
                Mockito.eq("testSingleton"),
                Mockito.argThat(new ClassMatcher(TestAnnotatedFactory.DummyComponent.class)),
                Mockito.any(Supplier.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredNamedComponentFactory() {
        scanner.scanForPackage(TestAnnotatedFactory.class.getPackage());
        Mockito.verify(factory).registerComponent(
            Mockito.eq("namedComponent"),
            Mockito.argThat(new ClassMatcher(TestAnnotatedFactory.DummyComponent.class)),
            Mockito.any(Supplier.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredSecondaryComponent() {
        scanner.scanForPackage(TestSecondaryComponent.class.getPackage());
        Mockito.verify(factory).registerComponent(
            Matchers.isNull(String.class),
            Mockito.argThat(new ClassMatcher(TestSecondaryComponent.class)),
            Mockito.any(Supplier.class),
            Mockito.any(ComponentDescriptorModifier.class));
    }

    @Test
    public void shouldScanAndRegisterComponentsFromExternalPath() throws IOException {

        generateTopologyProviderClass("1.0", "component-version-1");

        generateTopologyProviderClass("2.0", "component-version-2");

        scanner.scan(COMPONENT_PATH.toString());

        Optional<ComponentDescriptor<Object>> descriptorV10 = factory.findDescriptorByAlias(
                "test.ByteBuddyTopologyProvider", Qualifiers.byVersion("1.0"));

        assertTrue(Version.isEqual(descriptorV10.get().version(), "1.0"));

        Optional<ComponentDescriptor<Object>> descriptorV20 = factory.findDescriptorByAlias(
                "test.ByteBuddyTopologyProvider", Qualifiers.byVersion("2.0"));

        assertTrue(Version.isEqual(descriptorV20.get().version(), "2.0"));
    }

    private void generateTopologyProviderClass(final String version, final String path) throws IOException {
        new ByteBuddy()
            .subclass(TopologyProvider.class)
            .name("test.ByteBuddyTopologyProvider")
            .annotateType(AnnotationDescription.Builder.ofType(Component.class).build())
            .method(ElementMatchers.named("version"))
            .intercept(FixedValue.value(version))
            .make()
            .saveIn(new File(COMPONENT_PATH.toFile(), path));
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
}