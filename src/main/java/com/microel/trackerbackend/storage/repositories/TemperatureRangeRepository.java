package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TemperatureRangeRepository extends JpaRepository<TemperatureRange, Long>, JpaSpecificationExecutor<TemperatureRange> {
}
