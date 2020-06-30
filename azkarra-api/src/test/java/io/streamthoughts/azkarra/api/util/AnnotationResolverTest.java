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

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnotationResolverTest {

    @Test
    public void shouldResolvedRepeatableAnnotationGivenClass() {
        final List<String> annotations = AnnotationResolver.findAllAnnotationsByType(
            RepeatableTestClass.class,
            AnnotationResolverTest.TestAnnotation.class
        ).stream().map(AnnotationResolverTest.TestAnnotation::value).collect(Collectors.toList());
        assertEquals(2, annotations.size());
        assertTrue(annotations.containsAll(Arrays.asList("child1", "child2")));
    }

    @Test
    public void shouldResolvedInheritedAnnotationGivenClass() {
        final List<String> annotations = AnnotationResolver.findAllAnnotationsByType(
            InheritedTestClass.class,
            AnnotationResolverTest.TestAnnotation.class
        ).stream().map(AnnotationResolverTest.TestAnnotation::value).collect(Collectors.toList());
        assertEquals(1, annotations.size());
        assertTrue(annotations.contains("inherited"));
    }

    @Test
    public void shouldResolvedAnnotatedAnnotationGivenClass() {
        final List<String> annotations = AnnotationResolver.findAllAnnotationsByType(
            CustomTestClass.class,
            TestAnnotation.class
        ).stream().map(TestAnnotation::value).collect(Collectors.toList());
        assertEquals(1, annotations.size());
        assertTrue(annotations.contains("custom"));
    }

    @Test
    @CustomAnnotation
    public void shouldResolvedAnnotatedAnnotationGivenMethod() throws NoSuchMethodException {
        final List<String> annotations = AnnotationResolver.findAllAnnotationsByType(
            AnnotationResolverTest.class.getMethod("shouldResolvedAnnotatedAnnotationGivenMethod"),
            TestAnnotation.class
        ).stream().map(TestAnnotation::value).collect(Collectors.toList());
        assertEquals(1, annotations.size());
        assertTrue(annotations.contains("custom"));
    }

    @Test
    @TestAnnotation("child1")
    @TestAnnotation("child2")
    public void shouldResolvedRepeatableAnnotationGivenMethod() throws NoSuchMethodException {
        final List<String> annotations = AnnotationResolver.findAllAnnotationsByType(
            AnnotationResolverTest.class.getMethod("shouldResolvedRepeatableAnnotationGivenMethod"),
            AnnotationResolverTest.TestAnnotation.class
        ).stream().map(AnnotationResolverTest.TestAnnotation::value).collect(Collectors.toList());
        assertEquals(2, annotations.size());
        assertTrue(annotations.containsAll(Arrays.asList("child1", "child2")));
    }

    @Test
    public void shouldResolvedAllAnnotationsByUnwrappingContainerGivenClass() {
        final List<Annotation> annotations = AnnotationResolver.findAllAnnotations(RepeatableTestClass.class);
        assertEquals(2, annotations.size());

        final List<String> values = annotations
                .stream().map(annotation -> ((TestAnnotation)annotation).value())
                .collect(Collectors.toList());
        assertTrue(values.containsAll(Arrays.asList("child1", "child2")));
    }

    @Test
    public void shouldTestIfAnnotationIsOfType() {
        AnnotationResolver.isAnnotationOfType(() -> TestAnnotation.class, TestAnnotation.class);
    }

    @Inherited
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnnotations {
        TestAnnotation[] value();
    }

    @Inherited
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(AnnotationResolverTest.TestAnnotations.class)
    public @interface TestAnnotation {
        String value() default "";
    }

    @Inherited
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @TestAnnotation(value = "custom")
    @interface CustomAnnotation { }

    @CustomAnnotation
    private static class CustomTestClass  {
    }

    @TestAnnotation("inherited")
    static class SuperAnnotatedClass { }

    private static class InheritedTestClass extends SuperAnnotatedClass  { }

    @TestAnnotation("child1")
    @TestAnnotation("child2")
    private static class RepeatableTestClass  {
    }
}