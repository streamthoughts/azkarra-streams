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

import io.streamthoughts.azkarra.api.components.ComponentClassReader;
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.ComponentRegistry;
import io.streamthoughts.azkarra.streams.components.scantest.factory.TestComponentFactory;
import io.streamthoughts.azkarra.streams.components.scantest.test.TestAnnotatedComponent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class ClasspathComponentScannerTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredComponents() {
        ComponentClassReader reader = Mockito.mock(ComponentClassReader.class);
        ClasspathComponentScanner scanner = new ClasspathComponentScanner(reader, null);

        scanner.scan(TestAnnotatedComponent.class.getPackage());

        Mockito.verify(reader).registerComponent(
            Mockito.argThat(new ClassMatcher(TestAnnotatedComponent.class)),
            Mockito.any(ComponentRegistry.class) );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldScanAndRegisterDeclaredComponentFactory() {
        ComponentClassReader reader = Mockito.mock(ComponentClassReader.class);
        ClasspathComponentScanner scanner = new ClasspathComponentScanner(reader, null);

        scanner.scan(TestComponentFactory.class.getPackage());

        Mockito.verify(reader).registerComponent(
            Mockito.argThat(new ComponentFactoryMatcher(
                TestComponentFactory.DummyComponent.class,
                false)),
            Mockito.any(ComponentRegistry.class) );
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