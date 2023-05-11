package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.address.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {
    Optional<City> findFirstByName(String cityName);
}
