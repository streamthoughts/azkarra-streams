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
package io.streamthoughts.azkarra.api.config;

import io.streamthoughts.azkarra.api.errors.InvalidConfException;
import io.streamthoughts.azkarra.api.errors.ParsingConfException;
import io.streamthoughts.azkarra.api.util.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Conf} which is build from a list of arguments.
 * The arguments are keys optionally followed by values. Each key have to start with '-' or '--'.
 *
 * Keys with no values are interpreted as flags with value equals to {code true}.
 *
 */
public class ArgsConf extends MapConf {

    private static final String FLAG_NO_VALUE = "true";

    /**
     * Creates a new {@link ArgsConf} instance.
     *
     * @param args  the string arguments array.
     */
    public ArgsConf(final String[] args) {
        super(readArgs(args));
    }

    private static Map<String, Object> readArgs(final String[] args) {
        if (args.length == 0) return Collections.emptyMap();

        final Map<String, Object> config = new HashMap<>(args.length);
        int i = 0;
        while (i < args.length) {
            final String key = readArgKey(args, args[i]);

            i += 1; // move to value or next key

            if (i >= args.length) {
                config.put(key, FLAG_NO_VALUE);
            } else if (Utils.isNumber(args[i])) {
                config.put(key, args[i]);
                i += 1;
            } else if (args[i].startsWith("--") || args[i].startsWith("-")) {
                config.put(key, FLAG_NO_VALUE);
            } else {
                config.put(key, args[i]);
                i += 1;
            }
        }
        return config;
    }

    private static String readArgKey(final String[] args, final String arg) {
        String key;
        if (arg.startsWith("--")) {
            key = arg.substring(2);
        } else if (arg.startsWith("-")) {
            key = arg.substring(1);
        } else {
            throw new ParsingConfException(
                String.format(
                    "Error while parsing arguments '%s' on '%s'. Keys must be prefixed either with '--' or '-'.",
                    Arrays.toString(args), arg));
        }
        if (key.isEmpty()) {
            throw new InvalidConfException(
                "Passed arguments list contains an empty argument '" + Arrays.toString(args) + "'");
        }

        return key;
    }
}
