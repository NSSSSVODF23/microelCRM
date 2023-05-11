package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.CityRepository;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CityDispatcher {
    private final CityRepository cityRepository;


    public CityDispatcher(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<City> getCities() {
        return cityRepository.findAll();
    }

    public City getCity(Long cityId) throws EntryNotFound {
        return cityRepository.findById(cityId).orElseThrow(() -> new EntryNotFound("Город не найден"));
    }

    public Long getCount() {
        return cityRepository.count();
    }

    public City create(String name) {
        City city = new City();
        city.setName(name);
        city.setDeleted(false);
        return cityRepository.save(city);
    }

    public City createIfAbsent(String cityName) {
        City city = cityRepository.findFirstByName(cityName).orElse(null);
        if (city == null) {
            city = create(cityName);
        }
        return city;
    }

    public List<City> containsInName(String[] subQuery) {
        return cityRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String sub : subQuery) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + sub.toLowerCase() + "%"));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        });
    }
}
