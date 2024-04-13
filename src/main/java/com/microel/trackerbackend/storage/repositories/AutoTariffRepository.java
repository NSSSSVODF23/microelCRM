package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.tariff.AutoTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AutoTariffRepository extends JpaRepository<AutoTariff, Long>, JpaSpecificationExecutor<AutoTariff> {
}
