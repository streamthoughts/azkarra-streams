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
package io.streamthoughts.azkarra.serialization.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.streamthoughts.azkarra.api.AzkarraVersion;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.providers.TopologyDescriptor;
import io.streamthoughts.azkarra.api.streams.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.TaskMetadata;
import org.apache.kafka.streams.processor.ThreadMetadata;

/**
 * A simple module with all registered serializers.
 */
public class AzkarraSimpleModule extends SimpleModule {

    private static final String NAME = AzkarraSimpleModule.class.getSimpleName();

    private static final Version VERSION;

    static {
        VERSION = VersionUtil.parseVersion(AzkarraVersion.getVersion(), null, null);
    }

    /**
     * Creates a new {@link AzkarraSimpleModule} instance.
     */
    public AzkarraSimpleModule() {
        super(NAME, VERSION);
        addSerializer(Conf.class, new ConfSerializer());
        addSerializer(TopicPartition.class, new TopicPartitionSerializer());
        addSerializer(Windowed.class, new WindowedSerializer());
        addSerializer(TopologyDescriptor.class, new TopologyDescriptorSerializer());
        addSerializer(TaskMetadata.class, new TaskMetadataSerializer());
        addSerializer(ThreadMetadata.class, new ThreadMetadataSerializer());
        addSerializer(OffsetAndTimestamp.class, new OffsetAndTimestampSerializer());
    }
}
