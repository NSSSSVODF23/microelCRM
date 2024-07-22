package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.chart.TimeDataset;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.storage.dispatchers.TemperatureSensorsDispatcher;
import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureRange;
import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureSensor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/private/sensor")
public class SensorRequestController {

    private final TemperatureSensorsDispatcher temperatureSensorsDispatcher;

    public SensorRequestController(TemperatureSensorsDispatcher temperatureSensorsDispatcher) {
        this.temperatureSensorsDispatcher = temperatureSensorsDispatcher;
    }

    @GetMapping("temperature")
    public ResponseEntity<List<TemperatureSensor>> getTemperatureSensors() {
        return ResponseEntity.ok(temperatureSensorsDispatcher.getAllSensors());
    }

    @PostMapping("temperature")
    public ResponseEntity<Void> receiveTemperature(@RequestBody TemperatureSensorsDispatcher.SensorEvent event) {
        temperatureSensorsDispatcher.updateSensor(event);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("temperature/{id}")
    public ResponseEntity<Void> deleteTemperature(@PathVariable Long id) {
        temperatureSensorsDispatcher.deleteSensor(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("temperature/{id}/range")
    public ResponseEntity<Void> appendTemperatureRange(@PathVariable Long id, @RequestBody TemperatureRange range) {
        temperatureSensorsDispatcher.appendRange(id, range);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("temperature/range/{id}")
    public ResponseEntity<Void> editTemperatureRange(@PathVariable Long id, @RequestBody TemperatureRange range) {
        temperatureSensorsDispatcher.editRange(id, range);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("temperature/range/{id}")
    public ResponseEntity<Void> deleteTemperatureRange(@PathVariable Long id) {
        temperatureSensorsDispatcher.deleteRange(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Получить график сигнала температурного датчика
     * @param id Идентификатор температурного датчика
     * @param timeRange Диапазон времени
     */
    @PostMapping("temperature/{id}/chart")
    public ResponseEntity<TimeDataset>  getTemperatureSensorChart(@PathVariable Long id, @RequestBody DateRange timeRange) {
        return ResponseEntity.ok(temperatureSensorsDispatcher.getChart(id, timeRange));
    }
}
