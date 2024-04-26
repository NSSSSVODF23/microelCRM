package com.microel.trackerbackend.storage.entities.sensors.temperature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "temperature_ranges")
public class TemperatureRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long temperatureRangeId;
    private String name;
    private String color;
    private Float minTemp;
    private Float maxTemp;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_temperature_sensor_id")
    @JsonIgnore
    private TemperatureSensor sensor;

    public boolean isNotValid(){
        return minTemp == null || maxTemp == null || maxTemp <= minTemp ;
    }

    public TemperatureRange update(TemperatureRange range){
        setName(range.getName());
        setColor(range.getColor());
        setMinTemp(range.getMinTemp());
        setMaxTemp(range.getMaxTemp());
        return this;
    }
}
