package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TemperatureSensorRepository extends JpaRepository<TemperatureSensor, Long>, JpaSpecificationExecutor<TemperatureSensor> {
}
