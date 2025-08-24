package com.example.kafka;

import com.example.avro.PoisonRecord;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.errors.DeserializationHandlerResponse;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class StreamsDlqDeserializationHandler implements DeserializationExceptionHandler {

    private Producer<String, PoisonRecord> dlqProducer;

    private void initProducer() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class.getName());
        p.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        dlqProducer = new KafkaProducer<>(p);
    }

    @Override
    public DeserializationHandlerResponse handle(ProcessorContext context, ConsumerRecord<byte[], byte[]> record, Exception exception) {
        try {
            if (dlqProducer == null) initProducer();
            String key = record.key() == null ? null : new String(record.key(), StandardCharsets.UTF_8);
            PoisonRecord pr = PoisonRecord.newBuilder()
                    .setTopic(record.topic())
                    .setPartition(record.partition())
                    .setOffset(record.offset())
                    .setKey(key)
                    .setPayload(ByteBuffer.wrap(record.value()))
                    .setErrorMessage(exception.toString())
                    .setTs(System.currentTimeMillis())
                    .build();
            ProducerRecord<String, PoisonRecord> out = new ProducerRecord<>(record.topic() + ".DLT", key, pr);
            dlqProducer.send(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DeserializationHandlerResponse.CONTINUE;
    }

    @Override
    public void configure(java.util.Map<String, ?> configs) { }
}
