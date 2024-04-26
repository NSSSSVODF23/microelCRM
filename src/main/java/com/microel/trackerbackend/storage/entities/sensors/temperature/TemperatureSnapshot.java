package com.microel.trackerbackend.storage.entities.sensors.temperature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "temperature_snapshots")
public class TemperatureSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long temperatureSnapshotId;
    private Timestamp timestamp;
    private Float value;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_temperature_sensor_id")
    @JsonIgnore
    private TemperatureSensor sensor;

    public static TemperatureSnapshot of(TemperatureSensor sensor) {
        TemperatureSnapshot snapshot = new TemperatureSnapshot();
        final Timestamp now = Timestamp.from(Instant.now());
        snapshot.setSensor(sensor);
        snapshot.setTimestamp(now);
        snapshot.setValue(sensor.getValue());
        return snapshot;
    }
}
