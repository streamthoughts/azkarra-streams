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
import io.streamthoughts.azkarra.api.components.ComponentFactory;
import io.streamthoughts.azkarra.api.components.SimpleComponentDescriptor;
import io.streamthoughts.azkarra.api.config.Conf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class ConditionsTest {

    @Test
    public void shouldCompose() {
        Assertions.assertTrue(Conditions
            .compose(context -> true, context -> true)
            .matches(contextWith(Conf.empty()))
        );
    }

    @Test
    public void shouldReturnTrueWhenConditionOnPropertyEqualsGivenMatchingContext() {
        Assertions.assertTrue(Conditions
            .onPropertyEquals("props", "val")
            .matches(contextWith(Conf.with("props", "val")))
        );
    }

    @Test
    public void shouldReturnFalseWhenConditionOnPropertyEqualsGivenMatchingContext() {
        Assertions.assertFalse(Conditions
                .onPropertyEquals("props", "val")
                .matches(contextWith(Conf.empty()))
        );
    }

    @Test
    public void shouldReturnTrueWhenConditionOnPropertyExistGivenMatchingContext() {
        Assertions.assertTrue(Conditions
                .onPropertyExist("props")
                .matches(contextWith(Conf.with("props", "val")))
        );
    }

    @Test
    public void shouldReturnFalseWhenConditionOnPropertyExistGivenMatchingContext() {
        Assertions.assertFalse(Conditions
                .onPropertyExist("props")
                .matches(contextWith(Conf.empty()))
        );
    }

    @Test
    public void shouldReturnTrueWhenConditionOnPropertyTrueGivenMatchingContext() {
        Assertions.assertTrue(Conditions
                .onPropertyTrue("props")
                .matches(contextWith(Conf.with("props", "yes")))
        );
    }

    @Test
    public void shouldReturnFalseWhenConditionOnPropertyTrueGivenMatchingContext() {
        Assertions.assertFalse(Conditions
                .onPropertyTrue("props")
                .matches(contextWith(Conf.empty()))
        );
    }

    @Test
    public void shouldReturnTrueWhenConditionOnPropertyMissingGivenMatchingContext() {
        Assertions.assertTrue(Conditions
                .onPropertyMissing("props")
                .matches(contextWith(Conf.empty()))
        );
    }

    @Test
    public void shouldReturnFalseWhenConditionOnPropertyMissingGivenMatchingContext() {
        Assertions.assertFalse(Conditions
                .onPropertyMissing("props")
                .matches(contextWith(Conf.with("props", "")))
        );
    }

    @Test
    public void shouldReturnTrueWhenConditionOnPropertyMatchesGivenMatchingContext() {
        Assertions.assertTrue(Conditions
                .onPropertyMatches("props", "^val.*")
                .matches(contextWith(Conf.with("props", "value")))
        );
    }

    @Test
    public void shouldReturnFalseWhenConditionOnPropertyMatchesGivenMatchingContext() {
        Assertions.assertFalse(Conditions
                .onPropertyMatches("props", "^val.*")
                .matches(contextWith(Conf.with("props", "foo")))
        );
    }

    @Test
    public void shouldReturnTrueWhenConditionOnComponentGivenRegistered() {
        ComponentFactory mkFactory = Mockito.mock(ComponentFactory.class);
        ComponentDescriptor<ConditionsTest> descriptor = new SimpleComponentDescriptor<>("any", ConditionsTest.class, () -> null, true);
        Mockito.when(mkFactory.findAllDescriptorsByClass(Mockito.same(ConditionsTest.class))).thenReturn(List.of(descriptor));
        Assertions.assertTrue(Conditions
            .onComponents(List.of(ConditionsTest.class))
            .matches(contextWith(mkFactory))
        );
    }

    @Test
    public void shouldReturnFalseWhenConditionOnComponentGivenEmpty() {
        ComponentFactory mkFactory = Mockito.mock(ComponentFactory.class);
        Mockito.when(mkFactory.findAllDescriptorsByClass(Mockito.same(ConditionsTest.class))).thenReturn(Collections.emptyList());
        Assertions.assertFalse(Conditions
                .onComponents(List.of(ConditionsTest.class))
                .matches(contextWith(mkFactory))
        );
    }

    @Test
    public void shouldReturnFalseWhenConditionOnMissingComponentGivenRegistered() {
        ComponentFactory mkFactory = Mockito.mock(ComponentFactory.class);
        ComponentDescriptor<ConditionsTest> descriptor = new SimpleComponentDescriptor<>("any", ConditionsTest.class, () -> null, true);
        Mockito.when(mkFactory.findAllDescriptorsByClass(Mockito.same(ConditionsTest.class))).thenReturn(List.of(descriptor));
        Assertions.assertFalse(Conditions
                .onMissingComponent(List.of(ConditionsTest.class))
                .matches(contextWith(mkFactory))
        );
    }

    @Test
    public void shouldReturnTrueWhenConditionOnMissingComponentGivenEmpty() {
        ComponentFactory mkFactory = Mockito.mock(ComponentFactory.class);
        Mockito.when(mkFactory.findAllDescriptorsByClass(Mockito.same(ConditionsTest.class))).thenReturn(Collections.emptyList());
        Assertions.assertTrue(Conditions
                .onMissingComponent(List.of(ConditionsTest.class))
                .matches(contextWith(mkFactory))
        );
    }

    private ConditionContext contextWith(final ComponentFactory factory) {
        return contextWith(null, factory);
    }

    private ConditionContext contextWith(final Conf conf) {
        return contextWith(conf, null);
    }

    private ConditionContext contextWith(final Conf conf, final ComponentFactory factory) {
        return new ConditionContext() {
            @Override
            public ComponentFactory getComponentFactory() {
                return factory;
            }

            @Override
            public Conf getConfig() {
                return conf;
            }

            @Override
            public ComponentDescriptor getComponent() {
                return null;
            }
        };
    }

}