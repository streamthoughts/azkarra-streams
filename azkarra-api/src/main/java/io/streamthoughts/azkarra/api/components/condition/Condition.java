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

import java.util.function.Predicate;

/**
 * Define a single condition that need to be fulfilled for a component to be eligible for use in the application.
 */
@FunctionalInterface
public interface Condition extends Predicate<ConditionContext> {

    Condition True = new TrueCondition();

    /**
     * Verifies if the condition matches.
     *
     * @param context    the {@link ConditionContext}.
     *
     * @return           {@code true} if the component matches this condition, {@code false} otherwise.
     */
    boolean matches(final ConditionContext context);

    @Override
    default boolean test(final ConditionContext context) {
        return matches(context);
    }
}
