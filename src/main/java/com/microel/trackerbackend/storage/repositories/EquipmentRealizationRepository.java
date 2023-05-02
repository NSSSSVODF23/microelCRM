package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.storehouse.EquipmentRealization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EquipmentRealizationRepository extends JpaRepository<EquipmentRealization, Long>, JpaSpecificationExecutor<EquipmentRealization> {
}
