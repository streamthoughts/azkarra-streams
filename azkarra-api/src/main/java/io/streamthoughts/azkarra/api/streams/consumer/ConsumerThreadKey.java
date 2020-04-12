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
package io.streamthoughts.azkarra.api.streams.consumer;

import java.util.Objects;

public class ConsumerThreadKey {

    final String consumerThread;
    final String consumerClientId;

    ConsumerThreadKey(final String consumerThread,
                      final String consumerClientId) {
        this.consumerThread = consumerThread;
        this.consumerClientId = consumerClientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsumerThreadKey)) return false;
        ConsumerThreadKey that = (ConsumerThreadKey) o;
        return Objects.equals(consumerThread, that.consumerThread) &&
                Objects.equals(consumerClientId, that.consumerClientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerThread, consumerClientId);
    }
}
