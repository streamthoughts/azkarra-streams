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
package io.streamthoughts.azkarra.api.util;

import io.streamthoughts.azkarra.api.annotations.DefaultStreamsConfig;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ClassUtilsTest {

    @Test
    public void shouldResolvedRepeatableAnnotation() {
        final List<String> annotations = ClassUtils.getAllDeclaredAnnotationsByType(
            RepeatableTestClass.class,
            DefaultStreamsConfig.class
        ).stream().map(DefaultStreamsConfig::name).collect(Collectors.toList());
        assertEquals(2, annotations.size());
        assertTrue(annotations.containsAll(Arrays.asList("child1", "child2")));
    }

    @Test
    public void shouldResolvedInheritedAnnotation() {
        final List<String> annotations = ClassUtils.getAllDeclaredAnnotationsByType(
            InheritedTestClass.class,
            DefaultStreamsConfig.class
        ).stream().map(DefaultStreamsConfig::name).collect(Collectors.toList());
        assertEquals(1, annotations.size());
        assertTrue(annotations.contains("inherited"));
    }

    @Test
    public void shouldResolvedAnnotatedAnnotation() {
        final List<String> annotations = ClassUtils.getAllDeclaredAnnotationsByType(
            CustomTestClass.class,
            DefaultStreamsConfig.class
        ).stream().map(DefaultStreamsConfig::name).collect(Collectors.toList());
        assertEquals(1, annotations.size());
        assertTrue(annotations.contains("custom"));
    }

    @DefaultStreamsConfig(name = "inherited", value = "-")
    static class SuperAnnotatedClass { }

    private static class InheritedTestClass extends SuperAnnotatedClass  { }

    @DefaultStreamsConfig(name = "child1", value = "-")
    @DefaultStreamsConfig(name = "child2", value = "-")
    private static class RepeatableTestClass  {
    }

    @Inherited
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @DefaultStreamsConfig(name = "custom", value = "-")
    @interface CustomConfig { }

    @CustomConfig
    private static class CustomTestClass  {
    }
}