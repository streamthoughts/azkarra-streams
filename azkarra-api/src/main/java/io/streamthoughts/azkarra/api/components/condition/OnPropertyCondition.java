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
package io.streamthoughts.azkarra.api.components.condition;

import io.streamthoughts.azkarra.api.config.Conf;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Condition to express a requirement for a given property.
 */
public class OnPropertyCondition<T> implements Condition {

    private final Function<Conf, Optional<T>> accessor;

    private final Predicate<Optional<T>> predicate;

    static OnPropertyCondition<String> ofTypeString(final String property,
                                                    final Predicate<Optional<String>> predicate) {
        return new OnPropertyCondition<>(c -> c.getOptionalString(property), predicate);
    }

    static OnPropertyCondition<Boolean> ofTypeBoolean(final String property,
                                                      final Predicate<Optional<Boolean>> predicate) {
        return new OnPropertyCondition<>(c -> c.getOptionalBoolean(property), predicate);
    }

    /**
     * Creates a new {@link OnPropertyCondition} instance.
     *
     * @param accessor  the property accessor.
     * @param predicate the predicate the property should match.
     */
    public OnPropertyCondition(final Function<Conf, Optional<T>> accessor,
                               final Predicate<Optional<T>> predicate) {
        this.accessor = Objects.requireNonNull(accessor, "property cannot be null");
        this.predicate = predicate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final ConditionContext context) {
        Optional<T> optional = accessor.apply(context.getConfig());
        return predicate.test(optional);
    }
}
