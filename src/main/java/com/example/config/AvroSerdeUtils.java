package com.example.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.specific.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;

import java.util.Collections;
import java.util.Map;

public final class AvroSerdeUtils {
    public static <T extends SpecificRecord> SpecificAvroSerde<T> specificSerde(String schemaRegistryUrl, boolean isKey) {
        SpecificAvroSerde<T> serde = new SpecificAvroSerde<>();
        Map<String, String> conf = Collections.singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        serde.configure(conf, isKey);
        return serde;
    }
}
