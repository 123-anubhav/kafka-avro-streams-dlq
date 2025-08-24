package com.example.replay;

import com.example.avro.PoisonRecord;
import com.example.avro.VehicleLocation;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class DlqReplayCli {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-replay");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        props.put("specific.avro.reader", true);

        KafkaConsumer<String, PoisonRecord> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("vehicle_location.DLT"));

        Properties prodProps = new Properties();
        prodProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        prodProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        prodProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class.getName());
        prodProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        KafkaProducer<String, VehicleLocation> producer = new KafkaProducer<>(prodProps);

        while(true) {
            ConsumerRecords<String, PoisonRecord> recs = consumer.poll(Duration.ofSeconds(1));
            recs.forEach(r -> {
                PoisonRecord pr = r.value();
                // naive: skip actual decode, assume payload is vehicle location bytes
                System.out.println("DLQ record: " + pr.getErrorMessage() + " key:" + pr.getKey());
                // in real code decode payload bytes into VehicleLocation and re-publish
            });
        }
    }
}
