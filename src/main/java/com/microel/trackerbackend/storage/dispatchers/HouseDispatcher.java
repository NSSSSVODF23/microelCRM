package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.repositories.HouseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HouseDispatcher {
    private final HouseRepository houseRepository;

    public HouseDispatcher(HouseRepository houseRepository) {
        this.houseRepository = houseRepository;
    }

    public House createIfAbsent(House parsedHouse, Street streetEntity) {
        House houseEntity = houseRepository.findFirstByHouseNumAndFractionAndLetterAndBuildAndStreet(parsedHouse.getHouseNum(),
                parsedHouse.getFraction(),parsedHouse.getLetter(),parsedHouse.getBuild(),streetEntity).orElse(null);
        if (houseEntity == null) {
            parsedHouse.setStreet(streetEntity);
            houseEntity = houseRepository.save(parsedHouse);
        }
        return houseEntity;
    }

    public List<Address> getExistingAddresses(AddressDispatcher.AddressLookupRequest request) {
        if(request.getCityName() == null && request.getStreetName() == null){
            return new ArrayList<>();
        }
        return houseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
                Join<Object, Object> streetJoin = root.join("street", JoinType.LEFT);
                Join<Object,Object> cityJoin = streetJoin.join("city", JoinType.LEFT);
            if (request.getCityName() != null) {
                predicates.add(cb.like(cb.lower(cityJoin.get("name")), "%" + request.getCityName().toLowerCase() + "%"));
            }
            if (request.getStreetName() != null) {
                String streetQuery = "%" + request.getStreetName().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(streetJoin.get("name")), streetQuery),
                                cb.like(cb.lower(streetJoin.get("altNames")), streetQuery)
                        ));
                if (request.getHouseNum() != null) {
                    predicates.add(cb.equal(root.get("houseNum"), request.getHouseNum()));
                    if (request.getFraction() != null) {
                        predicates.add(cb.equal(root.get("fraction"), request.getFraction()));
                    }
                    if (request.getLetter() != null) {
                        predicates.add(cb.equal(root.get("letter"), request.getLetter()));
                    }
                    if (request.getBuild() != null) {
                        predicates.add(cb.equal(root.get("build"), request.getBuild()));
                    }
                }
            }
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().map(House::getAddress).collect(Collectors.toList());
    }
}
