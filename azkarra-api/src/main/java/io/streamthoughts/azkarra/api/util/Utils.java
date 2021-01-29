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
package io.streamthoughts.azkarra.api.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;

public class Utils {

    public static boolean isNullOrEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    public static boolean contains(char[] array, char target) {
        for (char value : array) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    public static String pruneSuffix(final String s, final String suffix) {
        int pos = s.lastIndexOf(suffix);
        if (pos > 0) {
            return s.substring(0, pos);
        }
        return s;
    }

    public static boolean isNumber(final String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) {
                    return false;
                } else {
                    continue;
                }
            }
            if (Character.digit(s.charAt(i), 10) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Instant.toEpochMilli() could result in an arithmetic overflow/underflow
     * (e.g. Instant.MAX.toEpochMilli() or Instant.MIN.toEpochMilli()).
     */
    public static Instant capped(final Instant instant) {
        Instant[] instants = {Instant.ofEpochMilli(Long.MIN_VALUE), instant, Instant.ofEpochMilli(Long.MAX_VALUE)};
        Arrays.sort(instants);
        return instants[1];
    }

    public static String formatStackTrace(final Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
