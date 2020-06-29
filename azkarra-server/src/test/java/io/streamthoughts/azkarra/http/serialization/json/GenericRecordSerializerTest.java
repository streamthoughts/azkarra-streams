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
package io.streamthoughts.azkarra.http.serialization.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.streamthoughts.azkarra.api.time.Time;
import io.streamthoughts.azkarra.example.User;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.Instant;

public class GenericRecordSerializerTest {

    private static final String SCHEMA =
            "{\"namespace\": \"avro.example\", " +
            "\"type\": \"record\",\"name\": \"User\"," +
            "\"fields\": [{\"name\": \"name\",\"type\": \"string\"}," +
            "{\"name\": \"created\",\"type\": {\"type\": \"long\",\"logicalType\": \"timestamp-millis\"}}]}";

    private static Instant NOW = Instant.now();

    @Test
    public void shouldSerializeAvroToJsonGivenGenericRecord() throws IOException {

        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(SCHEMA);
        GenericRecord record = new GenericData.Record(schema);
        record.put("name", "kafka");
        record.put("created", NOW.toEpochMilli());

        Writer jsonWriter = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
        SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
        new GenericRecordSerializer().serialize(record, jsonGenerator, serializerProvider);
        jsonGenerator.flush();
        Assertions.assertEquals(
            "{\"name\":\"kafka\",\"created\":" + NOW.toEpochMilli() + "}"
            , jsonWriter.toString());
    }

    @Test
    public void shouldSerializeAvroToJsonGivenSpecificRecord() throws IOException {
        User specificRecord = User.newBuilder().setName("kafka").setCreated(NOW).build();
        Writer jsonWriter = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
        SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
        new GenericRecordSerializer().serialize(specificRecord, jsonGenerator, serializerProvider);
        jsonGenerator.flush();
        Assertions.assertEquals(
            "{\"name\":\"kafka\",\"created\":" + NOW.toEpochMilli() + "}",
            jsonWriter.toString());
    }
}