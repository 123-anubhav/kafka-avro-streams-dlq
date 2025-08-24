package com.example.controller;

import com.example.avro.VehicleLocation;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProducerController {

    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    public ProducerController(KafkaTemplate<String, SpecificRecord> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/locations")
    public ResponseEntity<String> post(@RequestBody Map<String, Object> body) {
        VehicleLocation vl = VehicleLocation.newBuilder()
                .setVehicleId(body.get("vehicleId").toString())
                .setTs(((Number)body.get("ts")).longValue())
                .setLat(((Number)body.get("lat")).doubleValue())
                .setLon(((Number)body.get("lon")).doubleValue())
                .setSpeed(((Number)body.get("speed")).doubleValue())
                .build();
        kafkaTemplate.send("vehicle_location", vl.getVehicleId().toString(), vl);
        return ResponseEntity.accepted().body("ok");
    }
}
