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

package io.streamthoughts.azkarra.api.annotations;

import io.streamthoughts.azkarra.api.components.condition.Condition;
import io.streamthoughts.azkarra.api.components.condition.TrueCondition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to indicate the condition(s) that need to be fulfilled for a component to be eligible
 * for use in the application.
 */
@Documented
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Conditionals.class)
public @interface ConditionalOn {
    /**
     * Specify the name of the property that should be set.
     */
    String property() default "";

    /**
     * Specify the name of the property that should be missing.
     */
    String missingProperty() default "";

    /**
     * Specify the expected value for the {@link #property()}.
     */
    String havingValue() default "";

    /**
     * Specify the value that the {@link #property()} should not be equals to.
     */
    String notEquals() default "";

    /**
     * Specify the pattern that the {@link #property()} should match.
     */
    String matching() default "";

    /**
     * Specify that components of the given types must be registered for the component to be enabled.
     */
    Class[] components() default {};

    /**
     * Specify that components of the given types must be missing for the component to be enabled.
     */
    Class[] missingComponents() default {};

    /**
     * Specify one or more conditions that the component must match.
     */
    Class<? extends Condition>[] conditions() default TrueCondition.class;
}
