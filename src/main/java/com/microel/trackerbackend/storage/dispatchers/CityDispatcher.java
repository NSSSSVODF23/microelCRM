package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.DefaultCitiesConf;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.CityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CityDispatcher {
    private final CityRepository cityRepository;


    public CityDispatcher(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<City> getCities(){
        return cityRepository.findAll();
    }

    public City getCity(Long cityId) throws EntryNotFound {
        return cityRepository.findById(cityId).orElseThrow(()->new EntryNotFound("Город не найден"));
    }

    public Long getCount(){
        return cityRepository.count();
    }

    public City create(String name){
        City city = new City();
        city.setName(name);
        city.setDeleted(false);
        return cityRepository.save(city);
    }
}
