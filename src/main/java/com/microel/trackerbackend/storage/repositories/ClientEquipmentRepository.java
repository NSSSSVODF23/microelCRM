package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.equipment.ClientEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientEquipmentRepository extends JpaRepository<ClientEquipment, Long>, JpaSpecificationExecutor<ClientEquipment> {
}
