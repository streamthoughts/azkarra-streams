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
package io.streamthoughts.azkarra.streams.banner;

import io.streamthoughts.azkarra.api.banner.Banner;
import io.streamthoughts.azkarra.api.AzkarraVersion;

import java.io.PrintStream;

/**
 * The default {@link Banner} implementation which writes "Kafka Streams".
 */
public class AzkarraBanner implements Banner {

    private static final String[] BANNER = {"",
            "  _  __        __  _            ____   _                                     ",
            " | |/ / __ _  / _|| | __ __ _  / ___| | |_  _ __  ___   __ _  _ __ ___   ___ ",
            " | ' / / _` || |_ | |/ // _` | \\___ \\ | __|| '__|/ _ \\ / _` || '_ ` _ \\ / __|",
            " | . \\| (_| ||  _||   <| (_| |  ___) || |_ | |  |  __/| (_| || | | | | |\\__ \\",
            " |_|\\_\\\\__,_||_|  |_|\\_\\\\__,_| |____/  \\__||_|   \\___| \\__,_||_| |_| |_||___/", ""};

    private final String AZKARRA = " :: Azkarra :: ";

    private final int STRAP_LINE_SIZE = 77;

    /**
     * {@inheritDoc}
     */
    @Override
    public void print(final PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }

        String version = AzkarraVersion.getVersion();
        version = (version != null) ? " (v" + version + ")" : "";
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAP_LINE_SIZE - (version.length() + AZKARRA.length())) {
            padding.append(" ");
        }

        printStream.println(AZKARRA + padding + version);
        printStream.println();
    }
}
