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
package io.streamthoughts.azkarra;

import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.config.AzkarraConf;

/**
 * The default main class for starting an Azkarra worker.
 */
public class AzkarraWorker {

    public static void main(final String[] args) {
        final AzkarraConf azkarraConf = AzkarraConf.create();
        new AzkarraApplication()
            .setConfiguration(azkarraConf)
            .setEnableComponentScan(true)
            .setHttpServerEnable(true)
            .run(args);
    }
}
