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
package io.streamthoughts.azkarra.streams.autoconfigure;

import io.streamthoughts.azkarra.api.AzkarraContext;
import io.streamthoughts.azkarra.api.util.AnnotationResolver;
import io.streamthoughts.azkarra.runtime.context.DefaultAzkarraContext;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.AzkarraStreamsApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.ComponentScan;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.EnableAutoConfig;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.EnableAutoStart;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.EnableEmbeddedHttpServer;
import io.streamthoughts.azkarra.streams.config.AzkarraConf;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * This class is used for auto-configuring the {@link AzkarraApplication} instance.
 */
public class AutoConfigure {

    private static final Logger LOG = LoggerFactory.getLogger(AutoConfigure.class);

    /**
     * Creates a new {@link AutoConfigure} instance.
     */
    public AutoConfigure() {
    }

    public void load(final AzkarraApplication application) {
        LOG.info("Loading application configuration");
        Objects.requireNonNull(application, "application cannot be null");

        AzkarraContext context = application.getContext();

        if (context == null) {
            LOG.info("No AzkarraContext provided, initializing default provided implementation");
            application.setContext(DefaultAzkarraContext.create());
        }

        final Class<?> mainApplicationClass = application.getMainApplicationClass();
        final Optional<String> autoConfig = loadAutoConfigIfEnable(mainApplicationClass);
        autoConfig.ifPresent(configBasename -> {
            if (configBasename.isEmpty())
                // Load default configuration
                application.addConfiguration(AzkarraConf.create());
            else {
                // Load user defined custom configuration
                application.addConfiguration(AzkarraConf.create(configBasename));
            }
        });
        isHttpServerEnable(mainApplicationClass)
            .ifPresent(application::enableHttpServer);

        loadAutoStartEnvironmentNameIfEnable(mainApplicationClass)
            .ifPresent(env -> application.setAutoStart(true, env));

        isComponentScanEnable(mainApplicationClass)
            .ifPresent(application::setEnableComponentScan);
    }

    private static Optional<Boolean> isComponentScanEnable(final Class<?> source) {
        Set<Annotation> annotations = allAnnotationsOfType(source, ComponentScan.class);
        Optional<Boolean> optional = annotations.stream()
            .map(a -> ((ComponentScan) a).value())
            .findFirst();

        return optional.or(() -> isAnnotatedWith(source, AzkarraStreamsApplication.class) ?
            isHttpServerEnable(AzkarraStreamsApplication.class) : Optional.empty());
    }

    private static Optional<Boolean> isHttpServerEnable(final Class<?> source) {
        Set<Annotation> annotations = allAnnotationsOfType(source, EnableEmbeddedHttpServer.class);
        Optional<Boolean> optional = annotations.stream()
            .map(a -> ((EnableEmbeddedHttpServer) a).value())
            .findFirst();

        return optional.or(() -> isAnnotatedWith(source, AzkarraStreamsApplication.class) ?
            isHttpServerEnable(AzkarraStreamsApplication.class) : Optional.empty());
    }

    private static Optional<String> loadAutoStartEnvironmentNameIfEnable(final Class<?> source) {
        Set<Annotation> annotations = allAnnotationsOfType(source, EnableAutoStart.class);
        Optional<String> optional = annotations.stream()
           .map(a -> ((EnableAutoStart)a).environment())
           .findFirst();

        return optional.or(() -> isAnnotatedWith(source, AzkarraStreamsApplication.class) ?
                loadAutoStartEnvironmentNameIfEnable(AzkarraStreamsApplication.class) : Optional.empty());

    }

    private static Optional<String> loadAutoConfigIfEnable(final Class<?> source) {
        Set<Annotation> annotations = allAnnotationsOfType(source, EnableAutoConfig.class);
        Optional<String> optional = annotations.stream()
                .map(a -> ((EnableAutoConfig)a).name())
                .findFirst();

        return optional.or(() -> isAnnotatedWith(source, AzkarraStreamsApplication.class) ?
                loadAutoConfigIfEnable(AzkarraStreamsApplication.class) : Optional.empty());

    }

    private static <T extends AnnotatedElement> boolean isAnnotatedWith(
            final T type,
            final Class<? extends Annotation> annotation) {
        return !allAnnotationsOfType(type, annotation).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static <T extends AnnotatedElement> Set<Annotation> allAnnotationsOfType(
            final T type,
            final Class<? extends Annotation> annotation) {
        return ReflectionUtils.getAllAnnotations(type, a -> AnnotationResolver.isAnnotationOfType(a, annotation));
    }
}
