package com.example.streams;

import com.example.avro.VehicleLocation;
import com.example.streams.VehicleAvg;
import com.example.config.AvroSerdeUtils;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.specific.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

import java.util.Properties;

@Configuration
@EnableKafkaStreams
public class AvroStreamsConfig {

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public KStream<String, VehicleLocation> vehicleStream(StreamsBuilder builder) {
        SpecificAvroSerde<VehicleLocation> vehicleSerde = AvroSerdeUtils.specificSerde(schemaRegistryUrl, false);

        KStream<String, VehicleLocation> stream = builder.stream("vehicle_location", Consumed.with(Serdes.String(), vehicleSerde));
        stream = stream.selectKey((k, v) -> v.getVehicleId().toString());

        Properties pprops = new Properties();
        pprops.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        pprops.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        pprops.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class.getName());
        pprops.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        // safe processing (processing-time DLQ)
        stream = stream.transformValues(new com.example.kafka.SafeProcessor(pprops));

        // simple aggregate: sum & count as VehicleAvg (POJO)
        KTable<String, VehicleAvg> agg = stream
                .groupByKey(Grouped.with(Serdes.String(), vehicleSerde))
                .aggregate(
                        VehicleAvg::new,
                        (key, value, aggv) -> {
                            aggv.setSum(aggv.getSum() + value.getSpeed());
                            aggv.setCount(aggv.getCount() + 1);
                            return aggv;
                        },
                        Materialized.<String, VehicleAvg, KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>as("vehicle-avg-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new org.springframework.kafka.support.serializer.JsonSerde<>(VehicleAvg.class))
                );

        agg.toStream().mapValues(va -> va.getCount() == 0 ? 0.0 : va.getSum() / va.getCount())
                .to("vehicle_avg_speed", Produced.with(Serdes.String(), Serdes.Double()));

        return stream;
    }
}
