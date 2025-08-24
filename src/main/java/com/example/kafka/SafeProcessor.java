package com.example.kafka;

import com.example.avro.PoisonRecord;
import com.example.avro.VehicleLocation;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier;

import java.nio.ByteBuffer;
import java.util.Properties;

public class SafeProcessor implements ValueTransformerWithKeySupplier<String, VehicleLocation, VehicleLocation> {

    private final Properties producerProps;

    public SafeProcessor(Properties producerProps) {
        this.producerProps = producerProps;
    }

    @Override
    public ValueTransformerWithKey<String, VehicleLocation, VehicleLocation> get() {
        return new ValueTransformerWithKey<>() {
            private Producer<String, PoisonRecord> producer;
            private ProcessorContext context;

            @Override
            public void init(ProcessorContext context) {
                this.context = context;
                this.producer = new KafkaProducer<>(producerProps);
            }

            @Override
            public VehicleLocation transform(String readOnlyKey, VehicleLocation value) {
                try {
                    if (value.getSpeed() < 0) throw new IllegalArgumentException("Negative speed");
                    return value;
                } catch (Exception ex) {
                    PoisonRecord pr = PoisonRecord.newBuilder()
                            .setTopic(context.topic())
                            .setPartition(context.partition())
                            .setOffset(context.offset())
                            .setKey(readOnlyKey)
                            .setPayload(ByteBuffer.wrap(new byte[0]))
                            .setErrorMessage(ex.getMessage())
                            .setTs(System.currentTimeMillis())
                            .build();
                    producer.send(new ProducerRecord<>(context.topic() + ".DLT", readOnlyKey, pr));
                    return null;
                }
            }

            @Override
            public void close() {
                producer.close();
            }
        };
    }
}
