/*
 * Copyright 2021 StreamThoughts.
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
package io.streamthoughts.azkarra.commons.error;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.streamthoughts.azkarra.commons.error.DeadLetterTopicExceptionHandlerConfig.DLQ_RESPONSE_CONFIG;
import static io.streamthoughts.azkarra.commons.error.DeadLetterTopicExceptionHandlerConfig.prefixForProductionHandler;
import static io.streamthoughts.azkarra.commons.error.DeadLetterTopicExceptionHandlerTestUtils.TEST_PRODUCER_RECORD;
import static io.streamthoughts.azkarra.commons.error.DeadLetterTopicExceptionHandlerTestUtils.assertProducedRecord;
import static org.apache.kafka.streams.errors.ProductionExceptionHandler.ProductionExceptionHandlerResponse.CONTINUE;
import static org.apache.kafka.streams.errors.ProductionExceptionHandler.ProductionExceptionHandlerResponse.FAIL;

public class DeadLetterTopicProductionExceptionHandlerTest {

  @BeforeEach
  public void tearDown() {
    GlobalDeadLetterTopicCollector.clear();
  }

  @Test
  public void should_send_to_dlq_when_global_producer_is_configured() {
    MockProducer<byte[], byte[]> mkProducer =
        new MockProducer<>(true, new ByteArraySerializer(), new ByteArraySerializer());

    GlobalDeadLetterTopicCollector.getOrCreate(
        GlobalDeadLetterTopicCollectorConfig.create()
            .withProducer(mkProducer)
            .withAutoCreateTopicEnabled(false));

    var handler = new DeadLetterTopicProductionExceptionHandler();
    handler.configure(Map.of(StreamsConfig.APPLICATION_ID_CONFIG, "test-app"));

    var exception = new RecordTooLargeException("RecordTooLargeException");
    var response = handler.handle(TEST_PRODUCER_RECORD, exception);
    Assertions.assertEquals(FAIL, response);
    List<ProducerRecord<byte[], byte[]>> history = mkProducer.history();
    Assertions.assertFalse(history.isEmpty());
    assertProducedRecord(TEST_PRODUCER_RECORD, history.get(0), exception);
  }

  @Test
  public void should_not_send_to_dlq_when_global_producer_is_not_configured() {
    var handler = new DeadLetterTopicProductionExceptionHandler();
    handler.configure(Map.of(StreamsConfig.APPLICATION_ID_CONFIG, "test-app"));

    var response =
        handler.handle(
            TEST_PRODUCER_RECORD, new RecordTooLargeException("RecordTooLargeException"));
    Assertions.assertEquals(FAIL, response);
  }

  @Test
  public void should_return_continue_when_handler_is_configured() {
    var handler = new DeadLetterTopicProductionExceptionHandler();
    handler.configure(
        Map.of(
            StreamsConfig.APPLICATION_ID_CONFIG, "test-app",
            prefixForProductionHandler(DLQ_RESPONSE_CONFIG), ExceptionHandlerResponse.CONTINUE.name()
        )
    );

    var response =
        handler.handle(
            TEST_PRODUCER_RECORD, new RecordTooLargeException("RecordTooLargeException"));
    Assertions.assertEquals(CONTINUE, response);
  }
}
