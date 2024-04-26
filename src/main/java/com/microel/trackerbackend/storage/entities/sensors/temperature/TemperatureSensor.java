package com.microel.trackerbackend.storage.entities.sensors.temperature;

import com.microel.trackerbackend.storage.dispatchers.TemperatureSensorsDispatcher;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "temperature_sensors")
public class TemperatureSensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long temperatureSensorId;
    private Boolean active;
    private String name;
    private Float value;
    private Timestamp created;
    private Timestamp updated;
    @OneToOne
    @JoinColumn(name = "f_current_temperature_range_id")
    @Nullable
    private TemperatureRange currentRange;
    @OneToMany(mappedBy = "sensor")
    @OrderBy(value = "minTemp DESC")
    private List<TemperatureRange> ranges;

    public static TemperatureSensor create(TemperatureSensorsDispatcher.SensorEvent event) {
        TemperatureSensor sensor = new TemperatureSensor();
        final Timestamp now = Timestamp.from(Instant.now());
        sensor.setActive(true);
        sensor.setName(event.getName());
        sensor.setValue(event.getValue());
        sensor.setCreated(now);
        sensor.setUpdated(now);
        return sensor;
    }

    public void update(TemperatureSensorsDispatcher.SensorEvent event) {
        this.setActive(true);
        this.setUpdated(Timestamp.from(Instant.now()));
        this.setValue(event.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemperatureSensor that)) return false;
        return Objects.equals(getTemperatureSensorId(), that.getTemperatureSensorId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTemperatureSensorId());
    }
}
