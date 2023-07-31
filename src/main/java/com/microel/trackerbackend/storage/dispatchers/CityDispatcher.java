package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.CityRepository;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Component
public class CityDispatcher {
    private final CityRepository cityRepository;
    private final StompController stompController;


    public CityDispatcher(CityRepository cityRepository, StompController stompController) {
        this.cityRepository = cityRepository;
        this.stompController = stompController;
    }

    public List<City> getCities() {
        return cityRepository.findByDeletedIsFalseOrderByName();
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

    public City create(City.Form form) {
        if(!form.isValid()) throw new IllegalFields("Данные для создания города не валидны");
        boolean exists = cityRepository.existsByNameAndDeletedIsFalse(form.getName());
        if(exists) throw new AlreadyExists("Город с таким именем уже существует");
        City city = City.builder()
                .name(form.getName())
                .deleted(false)
                .build();
        City save = cityRepository.save(city);
        stompController.createCity(save);
        return city;
    }

    public City edit(Long cityId, City.Form form) {
        City city = cityRepository.findById(cityId).orElseThrow(() -> new EntryNotFound("Город не найден"));
        if(!form.isValid()) throw new IllegalFields("Данные для редактирования города не валидны");
        if(city.getName().equals(form.getName())) throw new IllegalFields("Изменения не внесены");
        boolean exists = cityRepository.existsByNameAndDeletedIsFalse(form.getName());
        if(exists && !city.getName().equals(form.getName())) throw new AlreadyExists("Город с таким именем уже существует");
        city.setName(form.getName());
        City save = cityRepository.save(city);
        stompController.updateCity(save);
        return save;
    }

    public City delete(Long cityId) {
        City city = cityRepository.findById(cityId).orElseThrow(() -> new EntryNotFound("Город не найден"));
        city.setDeleted(true);
        City save = cityRepository.save(city);
        stompController.deleteCity(save);
        return save;
    }
}
