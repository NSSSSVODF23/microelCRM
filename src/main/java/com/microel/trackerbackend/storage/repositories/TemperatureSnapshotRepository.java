package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.sensors.temperature.TemperatureSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TemperatureSnapshotRepository extends JpaRepository<TemperatureSnapshot, Long>, JpaSpecificationExecutor<TemperatureSnapshot> {
}
