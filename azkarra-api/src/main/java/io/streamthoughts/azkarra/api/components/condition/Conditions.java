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
package io.streamthoughts.azkarra.api.components.condition;

import io.streamthoughts.azkarra.api.annotations.ConditionalOn;
import io.streamthoughts.azkarra.api.util.ClassUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Conditions {

    /**
     * Static helper method to create a {@link Condition} compose of the given ones.
     *
     * @param conditions    the {@link Condition}s to compose.
     *
     * @return              the new {@link Condition} object.
     */
    public static Condition compose(final List<? extends Condition> conditions) {
        return new CompositeCondition(conditions);
    }

    /**
     * Static helper method to create a {@link Condition} compose of the given ones.
     *
     * @param conditions    the {@link Condition}s to compose.
     *
     * @return              the new {@link Condition} object.
     */
    public static Condition compose(final Condition ...conditions) {
        return new CompositeCondition(List.of(conditions));
    }


    /**
     * Specify that the given property should be set for a component to be eligible for use.
     * .
     * @param property  the property name.
     * @return          the {@link Condition} object.
     */
    public static Condition onPropertyExist(final String property) {
        return OnPropertyCondition.ofTypeString(property, Optional::isPresent);
    }

    /**
     * Specify that the given property should be missing for a component to be eligible for use.
     *
     * @param property  the property name.
     * @return          the {@link Condition} object.
     */
    public static Condition onPropertyMissing(final String property) {
        return OnPropertyCondition.ofTypeString(property, Optional::isEmpty);
    }

    /**
     * Specify that the given property should be equal to the given value for a component to be eligible for use.
     *
     * @param property  the property name.
     * @return          the {@link Condition} object.
     */
    public static Condition onPropertyEquals(final String property, final String value) {
        final Predicate<Optional<String>> predicate = opt -> opt.map(s -> s.equals(value)).orElse(false);
        return OnPropertyCondition.ofTypeString(property, predicate);
    }

    /**
     * Specify that the given property should be equal to the given value for a component to be eligible for use.
     *
     * @param property  the property name.
     * @return          the {@link Condition} object.
     */
    public static Condition onPropertyNotEquals(final String property, final String value) {
        final Predicate<Optional<String>> predicate = opt -> opt.map(s -> !s.equals(value)).orElse(false);
        return OnPropertyCondition.ofTypeString(property, predicate);
    }

    /**
     * Specify that the given property should be matched the given pattern for a component to be eligible for use.
     *
     * @param property  the property name.
     * @return          the {@link Condition} object.
     */
    public static Condition onPropertyMatches(final String property, final String pattern) {
        var match = Pattern.compile(pattern).asMatchPredicate();
        return OnPropertyCondition.ofTypeString(property, opt -> opt.map(match::test).orElse(false));
    }

    /**
     * Specify that the given property should be {@code true} for a component to be eligible for use.
     *
     * @param property  the property name.
     * @return          the {@link Condition} object.
     */
    public static Condition onPropertyTrue(final String property) {
        return OnPropertyCondition.ofTypeBoolean(property, opt -> opt.orElse(false));
    }

    /**
     * Specify that components of the given types must be registered for the component to be enabled.
     *
     * @param types  the components that should be registered.
     * @return       the {@link Condition} object.
     */
    public static Condition onComponents(final List<Class> types) {
        return new OnComponentCondition(types, true);
    }

    /**
     * Specify that components of the given types must be missing for the component to be enabled.
     *
     * @param types  the components that should be missing.
     * @return       the {@link Condition} object.
     */
    public static Condition onMissingComponent(final List<Class> types) {
        return new OnComponentCondition(types, false);
    }

    /**
     * Composite condition.
     */
    private static class CompositeCondition implements Condition {

        final List<? extends Condition> conditions;

        private CompositeCondition(final List<? extends Condition> conditions) {
            this.conditions = Objects.requireNonNull(conditions);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(final ConditionContext context) {
            return conditions.stream().allMatch(cond -> cond.matches(context));
        }
    }

    /**
     * Static helper to build a list of {@link Condition}s based on the given annotation.
     *
     * @param annotations   the {@link ConditionalOn} annotations.
     * @return              the {@link Condition}s.
     */
    public static List<Condition> buildConditionsForAnnotations(final List<ConditionalOn> annotations) {
        List<Condition> allConditions = new LinkedList<>();
        for (ConditionalOn conditional : annotations) {
            for (Class<? extends Condition> conditionClass : conditional.conditions()) {
                allConditions.add(ClassUtils.newInstance(conditionClass));
            }
            final var components = Arrays.asList(conditional.components());
            if (!components.isEmpty()) {
                allConditions.add(Conditions.onComponents(components));
            }

            final var missingComponents = Arrays.asList(conditional.missingComponents());
            if (!missingComponents.isEmpty()) {
                allConditions.add(Conditions.onMissingComponent(missingComponents));
            }

            final var property = conditional.property();
            if (!property.isEmpty()) {
                if (!conditional.havingValue().isEmpty()) {
                    allConditions.add(Conditions.onPropertyEquals(property, conditional.havingValue()));
                }
                if (!conditional.matching().isEmpty()) {
                    allConditions.add(Conditions.onPropertyMatches(property, conditional.matching()));
                }
                if (!conditional.notEquals().isEmpty()) {
                    allConditions.add(Conditions.onPropertyNotEquals(property, conditional.notEquals()));
                }
            }
            if (!conditional.missingProperty().isEmpty()) {
                allConditions.add(Conditions.onPropertyMissing(property));
            }
        }
        return allConditions;
    }

}
