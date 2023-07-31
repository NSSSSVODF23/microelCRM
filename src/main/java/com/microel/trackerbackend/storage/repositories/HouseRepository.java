package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<House, Long>, JpaSpecificationExecutor<House> {
    Optional<House> findFirstByHouseNumAndFractionAndLetterAndBuildAndStreet(Short houseNum, Short fraction, Character letter, Short build, Street street);
    List<House> findByStreet_StreetIdAndDeletedFalseOrderByHouseNum(Long streetId);
}
