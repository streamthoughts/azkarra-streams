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

import io.streamthoughts.azkarra.api.annotations.Restricted;
import io.streamthoughts.azkarra.api.components.ComponentDescriptor;
import io.streamthoughts.azkarra.api.components.Qualifier;
import io.streamthoughts.azkarra.api.components.Restriction;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class RestrictionQualifier<T> implements Qualifier<T> {

    private static final String RESTRICTED_ATTRIBUTE = Restricted.class.getSimpleName();
    public static final String TYPE_MEMBER = "type";
    public static final String NAMES_MEMBER = "names";

    private final Restriction restriction;

    /**
     * Creates a new {@link RestrictionQualifier} instance.
     *
     * @param restriction   the {@link Restriction} instance.
     */
    RestrictionQualifier(final Restriction restriction) {
        this.restriction = Objects.requireNonNull(restriction, "restriction cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<ComponentDescriptor<T>> filter(final Class<T> componentType,
                                                 final Stream<ComponentDescriptor<T>> candidates) {
        return candidates.filter(this::isRestricted);
    }

    private boolean isRestricted(final ComponentDescriptor<T> candidate) {
        final String restrictionType = candidate
                .metadata()
                .stringValue(RESTRICTED_ATTRIBUTE, TYPE_MEMBER);

        if (restrictionType == null)
            return restriction.type().equals(Restriction.TYPE_APPLICATION);

        if (!restrictionType.equals(restriction.type()))
            return false;

        final String[] names = candidate.metadata()
                .arrayValue(RESTRICTED_ATTRIBUTE, NAMES_MEMBER);

        return Arrays.asList(names).contains(restriction.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestrictionQualifier)) return false;
        RestrictionQualifier<?> that = (RestrictionQualifier<?>) o;
        return Objects.equals(restriction, that.restriction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(restriction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "@Restricted(type=" + restriction.type() + ", name=" + restriction.name() + ")";
    }
}
