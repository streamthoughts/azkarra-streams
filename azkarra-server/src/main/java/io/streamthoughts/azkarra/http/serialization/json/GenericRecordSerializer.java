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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.streamthoughts.azkarra.serialization.SerializationException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GenericRecordSerializer extends JsonSerializer<GenericRecord> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final GenericRecord record,
                          final JsonGenerator gen,
                          final SerializerProvider serializers) throws IOException {

        gen.writeRaw(getJsonString(record, record.getSchema()));
    }

    private static String getJsonString(final GenericRecord record, final Schema schema) {
        try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, os);
            final DatumWriter<GenericRecord> writer;
            if (record instanceof SpecificRecord) {
                writer = new SpecificDatumWriter<>();
            } else {
                writer = new GenericDatumWriter<>();
            }
            writer.setSchema(schema);
            writer.write(record, encoder);
            encoder.flush();
            return new String(os.toByteArray(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new SerializationException(
                "Error occurred while serializing Avro record of type" + record.getClass().getName(),
                e
            );
        }
    }
}
