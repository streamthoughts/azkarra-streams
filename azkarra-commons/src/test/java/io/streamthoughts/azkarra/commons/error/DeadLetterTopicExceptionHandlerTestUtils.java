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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_APPLICATION_ID;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_EXCEPTION_CLASS_NAME;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_EXCEPTION_MESSAGE;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_EXCEPTION_STACKTRACE;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_RECORD_OFFSET;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_RECORD_PARTITION;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_RECORD_TOPIC;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_TIMESTAMP;
import static io.streamthoughts.azkarra.commons.error.ExceptionHeaders.ERROR_TYPE;
import static io.streamthoughts.azkarra.commons.error.ExceptionType.DESERIALIZATION;
import static io.streamthoughts.azkarra.commons.error.ExceptionType.PRODUCTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeadLetterTopicExceptionHandlerTestUtils {

  public static final String TEST_TOPIC = "test-topic";
  public static final String TEST_APP = "test-app";
  public static final String TEST_RECORD_KEY = "test-key";
  public static final String TEST_RECORD_VALUE = "test-value";

  public static final Headers TEST_HEADERS =
      new RecordHeaders()
          .add("test-header-key", "test-header-value".getBytes(StandardCharsets.UTF_8));

  static final ProducerRecord<byte[], byte[]> TEST_PRODUCER_RECORD =
      new ProducerRecord<>(
          DeadLetterTopicExceptionHandlerTestUtils.TEST_TOPIC,
          0,
          TEST_RECORD_KEY.getBytes(StandardCharsets.UTF_8),
          TEST_RECORD_VALUE.getBytes(StandardCharsets.UTF_8),
          TEST_HEADERS);

  static final ConsumerRecord<byte[], byte[]> TEST_CONSUMER_RECORD =
      new ConsumerRecord<>(
          DeadLetterTopicExceptionHandlerTestUtils.TEST_TOPIC,
          0,
          1235L,
          0L,
          TimestampType.CREATE_TIME,
          0L,
          ConsumerRecord.NULL_SIZE,
          ConsumerRecord.NULL_SIZE,
          TEST_RECORD_KEY.getBytes(StandardCharsets.UTF_8),
          TEST_RECORD_VALUE.getBytes(StandardCharsets.UTF_8),
          TEST_HEADERS);

  public static void assertProducedRecord(
      final ProducerRecord<byte[], byte[]> source,
      final ProducerRecord<byte[], byte[]> record,
      final Exception e) {

    Map<String, String> headers = assertCommonsHeadersAndGet(record, e);

    assertEquals(source.topic(), headers.get(ERROR_RECORD_TOPIC));
    assertEquals(String.valueOf(source.partition()), headers.get(ERROR_RECORD_PARTITION));

    assertEquals(PRODUCTION.name(), headers.get(ERROR_TYPE));
  }

  public static void assertProducedRecord(
      final ConsumerRecord<byte[], byte[]> source,
      final ProducerRecord<byte[], byte[]> record,
      final Exception e) {

    Map<String, String> headers = assertCommonsHeadersAndGet(record, e);

    assertEquals(source.topic(), headers.get(ERROR_RECORD_TOPIC));
    assertEquals(String.valueOf(source.partition()), headers.get(ERROR_RECORD_PARTITION));
    assertEquals(String.valueOf(source.offset()), headers.get(ERROR_RECORD_OFFSET));

    assertEquals(DESERIALIZATION.name(), headers.get(ERROR_TYPE));
  }

  private static Map<String, String> assertCommonsHeadersAndGet(
      final ProducerRecord<byte[], byte[]> record, final Exception e) {

    assertEquals(TEST_TOPIC + DefaultDeadLetterTopicNameExtractor.DEFAULT_SUFFIX, record.topic());

    final Map<String, String> headers =
        Stream.of(record.headers().toArray())
            .collect(Collectors.toMap(Header::key, h -> new String(h.value())));

    assertEquals(TEST_APP, headers.get(ERROR_APPLICATION_ID));
    assertEquals(e.getMessage(), headers.get(ERROR_EXCEPTION_MESSAGE));
    assertEquals(e.getClass().getName(), headers.get(ERROR_EXCEPTION_CLASS_NAME));
    assertEquals(ExceptionHeaders.getStacktrace(e), headers.get(ERROR_EXCEPTION_STACKTRACE));
    assertNotNull(headers.get(ERROR_TIMESTAMP));
    assertEquals("test-header-value", headers.get("test-header-key"));

    return headers;
  }
}
