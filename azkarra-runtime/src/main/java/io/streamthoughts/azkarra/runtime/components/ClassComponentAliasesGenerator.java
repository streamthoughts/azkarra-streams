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
package io.streamthoughts.azkarra.runtime.components;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.ComponentAliasesGenerator;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.api.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * {@link ClassComponentAliasesGenerator} can be used to generate aliases from component class.
 */
public class ClassComponentAliasesGenerator implements ComponentAliasesGenerator {

    private static final ClassAliasExtractor DEFAULT_LIAS_EXTRACTOR =
            new DropClassNameSuffixExtractor("Provider", cls -> true);

    private final List<ClassAliasExtractor> extractors;

    /**
     * Creates a new {@link ClassComponentAliasesGenerator} instance.
     */
    public ClassComponentAliasesGenerator() {
        this.extractors = new ArrayList<>();
        addClassAliasExtractor(DEFAULT_LIAS_EXTRACTOR);
        addClassAliasExtractor(
            new DropClassNameSuffixExtractor("TopologyProvider", TopologyProvider.class::isAssignableFrom)
        );
        addClassAliasExtractor(
            new DropClassNameSuffixExtractor("Topology", TopologyProvider.class::isAssignableFrom)
        );
    }

    public void addClassAliasExtractor(final ClassAliasExtractor extractor) {
        Objects.requireNonNull(extractor, "extractor cannot be null");
        this.extractors.add(extractor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAliasesFor(final ComponentDescriptor descriptor,
                                     final Collection<? extends ComponentDescriptor> allDescriptors) {
        final Set<String> aliases = computeAllAliasesFor(descriptor, extractors);
        boolean match = false;
        for (ComponentDescriptor other : allDescriptors) {
            final Set<String> otherAliases = computeAllAliasesFor(other, extractors);
            if (aliases.equals(otherAliases)) {
                if (match) {
                    return Collections.emptySet();
                }
                match = true;
            } else {
                for (final String otherAlias : otherAliases) {
                    if (aliases.contains(otherAlias)) {
                        if (match) {
                            return Collections.emptySet();
                        }
                        match = true;
                        break;
                    }
                }
            }
        }
        return aliases;
    }

    private Set<String> computeAllAliasesFor(final ComponentDescriptor component,
                                             final Collection<? extends ClassAliasExtractor> extractors) {

        final Class<?> providerClass = component.type();
        final String simpleName = providerClass.getSimpleName();
        final Set<String> aliases = new HashSet<>();
        aliases.add(simpleName);
        for (ClassAliasExtractor extractor : extractors) {
            if (extractor.accept(providerClass)) {
                aliases.add(extractor.extractAlias(providerClass));
            }
        }
        return new TreeSet<>(aliases);
    }

    public interface ClassAliasExtractor {

        boolean accept(final Class<?> componentClass);

        String extractAlias(final Class<?> componentClass);
    }

    public static class DropClassNameSuffixExtractor implements ClassAliasExtractor {

        private final String suffix;
        private final Predicate<Class<?>> accept;

        DropClassNameSuffixExtractor(final String suffix,
                                     final Predicate<Class<?>> accept) {
            this.suffix = suffix;
            this.accept = accept;
        }

        @Override
        public boolean accept(Class<?> componentClass) {
            return accept.test(componentClass);
        }

        @Override
        public String extractAlias(final Class<?> componentClass) {
            return Utils.pruneSuffix(componentClass.getSimpleName(), suffix);
        }
    }
}
