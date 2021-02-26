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
package io.streamthoughts.azkarra.api.streams;

/**
 * Interface to be implemented by any object that wishes to be notified of
 * the {@link KafkaStreamsContainer} that it runs in.
 *
 * This interface can be implemented by any object that implemented:
 *
 * @see org.apache.kafka.streams.KafkaStreams.StateListener
 * @see org.apache.kafka.streams.processor.StateRestoreListener
 * @see KafkaStreamsFactory
 *
 */
public interface KafkaStreamsContainerAware  {

    /**
     * Sets the KafkaStreamsContainer that this object runs in.
     *
     * @param container the {@link KafkaStreamsContainer}.
     */
    void setKafkaStreamsContainer(final KafkaStreamsContainer container);

}
