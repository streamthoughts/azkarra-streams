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

import io.streamthoughts.azkarra.api.components.ComponentDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class OnComponentCondition implements Condition {

    private final List<Class> types;
    private final boolean exists;

    /**
     * Creates a new {@link OnComponentCondition} instance.
     *
     * @param exists
     */
    public OnComponentCondition(final List<Class> types, final boolean exists) {
        this.exists = exists;
        this.types = types;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final ConditionContext context) {
        return exists ? verifyExists(context) : verifyIsMissing(context);
    }

    private boolean verifyExists(ConditionContext context) {
        for (Class type : types) {
            // verify that a component is available for the given type.
            if (verify(type, context, true)) {
                return true;
            }
        }
        return false;
    }

    private boolean verifyIsMissing(ConditionContext context) {
        for (Class type : types) {
            // verify that no component is available for the given type.
            if (!verify(type, context, false)) {
                return false;
            }
        }
        return true;
    }

    private boolean verify(final Class type,
                           final ConditionContext context,
                           final boolean exists) {
        Collection<ComponentDescriptor> descriptors  = context.getComponentFactory().findAllDescriptorsByClass(type);
        for (ComponentDescriptor descriptor : descriptors) {
            // don't re-evaluate the component it-self
            if (!context.getComponent().equals(descriptor)) {
                Optional<Condition> optionalCond = descriptor.condition();
                if (optionalCond.isPresent()) {
                    Condition condition = optionalCond.get();
                    if (condition.matches(context)) {
                        return exists;
                    }
                } else {
                    return exists;
                }
            }
        }
        return !exists;
    }
}
