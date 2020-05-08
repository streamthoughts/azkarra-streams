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

package io.streamthoughts.azkarra.api.annotations;

/**
 * Annotation that can be used on any class or method directly or indirectly annotated with {@link Component}.
 * to indicate that a component is to be eagerly initialized.
 *
 * By default, Azkarra lazily instantiates and configures a component when it is first requested.
 * Generally, this is the behavior desired by users because the contextual configuration of a
 * component cannot be resolved at context startup.
 */
public @interface Eager {

    boolean value() default true;
}
