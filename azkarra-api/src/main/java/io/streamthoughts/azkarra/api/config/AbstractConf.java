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
package io.streamthoughts.azkarra.api.config;

import io.streamthoughts.azkarra.api.errors.InvalidConfException;
import io.streamthoughts.azkarra.api.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractConf implements Conf {

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getClasses(final String key, final Class<T> cls) {
        List<String> classes = getStringList(key);

        Collection<T> instantiated = new ArrayList<>(classes.size());
        for (String clazz : classes) {
            final T instance = newInstance(clazz.trim(), cls);
            instantiated.add(instance);
        }
        return instantiated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getClass(final String key, final Class<T> cls) {
        final String trimmed = getString(key).trim();
        return newInstance(trimmed, cls);
    }

    private <T> T newInstance(final String cls, final Class<T> expected) {
        try {
            final Class<?> c = Class.forName(cls, true, ClassUtils.getClassLoader());
            return newInstance(c, expected);
        } catch (ClassNotFoundException e) {
            throw new InvalidConfException("Error while creating new instance of " + cls, e);
        }
    }

    private <T> T newInstance(final Class<?> c, final Class<T> expected) {
        if (c == null)
            return null;
        Object o = ClassUtils.newInstance(c);
        if (!expected.isInstance(o))
            throw new InvalidConfException(
                    c.getName() + " is not an instance of " + expected.getName()
            );
        return expected.cast(o);
    }
}
