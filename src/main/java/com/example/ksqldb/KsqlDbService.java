package com.example.ksqldb;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;
import io.confluent.ksql.api.client.QueryResult;
import io.confluent.ksql.api.client.Row;
import io.confluent.ksql.api.client.StreamedQueryResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KsqlDbService {
    private final Client client;

    public KsqlDbService(@Value("${ksqldb.host}") String host, @Value("${ksqldb.port}") int port) {
        ClientOptions opts = ClientOptions.create().setHost(host).setPort(port);
        client = Client.create(opts);
    }

    public CompletableFuture<Void> createViews() {
        String s1 = "CREATE STREAM VEHICLE_LOCATION_STREAM (VEHICLEID VARCHAR KEY, TS BIGINT, LAT DOUBLE, LON DOUBLE, SPEED DOUBLE) WITH (KAFKA_TOPIC='vehicle_location', VALUE_FORMAT='AVRO');";
        String s2 = "CREATE TABLE HIGH_SPEED_ALERTS AS SELECT VEHICLEID, MAX(SPEED) AS MAX_SPEED FROM VEHICLE_LOCATION_STREAM WINDOW TUMBLING (SIZE 1 MINUTES) GROUP BY VEHICLEID HAVING MAX(SPEED) > 80;";
        return client.executeStatement(s1).thenCompose(r->client.executeStatement(s2)).thenApply(r->null);
    }

    public CompletableFuture<Void> pushQuery() {
        return client.streamQuery("SELECT VEHICLEID, MAX_SPEED FROM HIGH_SPEED_ALERTS EMIT CHANGES;")
                .thenAccept(res -> res.subscribe(row -> System.out.println(row)));
    }

    public CompletableFuture<QueryResult> pullQuery(String vehicleId) {
        String q = "SELECT MAX_SPEED FROM HIGH_SPEED_ALERTS WHERE VEHICLEID='"+vehicleId+"';";
        return client.executeQuery(q);
    }
}
