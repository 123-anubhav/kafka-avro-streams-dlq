package com.example.controller;

import com.example.streams.VehicleAvg;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.StoreQueryParameters;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StateQueryController {

    private final KafkaStreams kafkaStreams;

    public StateQueryController(KafkaStreams kafkaStreams) {
        this.kafkaStreams = kafkaStreams;
    }

    @GetMapping("/avg/{vehicleId}")
    public ResponseEntity<Double> getAvg(@PathVariable String vehicleId) {
        ReadOnlyKeyValueStore<String, VehicleAvg> store = kafkaStreams.store(StoreQueryParameters.fromNameAndType("vehicle-avg-store", QueryableStoreTypes.keyValueStore()));
        VehicleAvg v = store.get(vehicleId);
        if (v==null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(v.getCount()==0?0.0:v.getSum()/v.getCount());
    }
}
