package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.equipment.ClientEquipmentRealization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientEquipmentRealizationRepository extends JpaRepository<ClientEquipmentRealization, Long>, JpaSpecificationExecutor<ClientEquipmentRealization> {
}
