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
package io.streamthoughts.azkarra.api.components.qualifier;

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.Qualifier;

import java.util.Objects;
import java.util.stream.Stream;

public class NamedQualifier<T> implements Qualifier<T> {

    private final String name;

    /**
     * Creates a new {@link NamedQualifier} instance.
     *
     * @param name  the component name.
     */
    NamedQualifier(final String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<ComponentDescriptor<T>> filter(final Class<T> componentType,
                                                 final Stream<ComponentDescriptor<T>> candidates) {
        return candidates.filter(descriptor -> descriptor.name().equalsIgnoreCase(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamedQualifier)) return false;
        NamedQualifier<?> that = (NamedQualifier<?>) o;
        return name.equals(that.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "@Named(" + name + ")";
    }
}
