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
package io.streamthoughts.azkarra.api.query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum StoreOperation {

    GET, ALL, FETCH, FETCH_KEY_RANGE, FETCH_TIME_RANGE, FETCH_ALL, RANGE, COUNT;

    private static final Map<String, StoreOperation> CACHE = new HashMap<>();

    static {
        Arrays.stream(StoreOperation.values())
              .forEach(o -> CACHE.put(o.name(), o));
    }

    public static Optional<StoreOperation> parse(final String operation) {
        return Optional.ofNullable(CACHE.get(operation.toUpperCase()));
    }

    public String prettyName() {
        return name().toLowerCase();
    }
}
