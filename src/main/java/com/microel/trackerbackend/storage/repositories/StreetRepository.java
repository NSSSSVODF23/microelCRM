package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.Street;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StreetRepository extends JpaRepository<Street, Long>, JpaSpecificationExecutor<Street> {
    List<Street> findAllByCity_CityId(Long cityId);

    Optional<Street> findByName(String name);

    List<Street> findByNameLikeIgnoreCaseAndCity_CityId(String substring, Long cityId);

    Optional<Street> findFirstByNameContainingIgnoreCaseAndCity(String name, City city);
}
