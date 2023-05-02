package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.storehouse.StoreHouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StoreHouseRepository extends JpaRepository<StoreHouse, Long>, JpaSpecificationExecutor<StoreHouse> {
}
