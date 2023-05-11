package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.DefaultCitiesConf;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.repositories.StreetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StreetDispatcher {
    private final StreetRepository streetRepository;
    private final CityDispatcher cityDispatcher;
    private final ConfigurationStorage configurationStorage;

    public StreetDispatcher(StreetRepository streetRepository, CityDispatcher cityDispatcher, ConfigurationStorage configurationStorage) {
        this.streetRepository = streetRepository;
        this.cityDispatcher = cityDispatcher;
        this.configurationStorage = configurationStorage;
        if (cityDispatcher.getCount() == 0L) {
            try {
                DefaultCitiesConf conf = configurationStorage.load(DefaultCitiesConf.class);
                conf.forEach(cityDef -> {
                    City city = cityDispatcher.create(cityDef.name);
                    saveAll(cityDef.streets, city);
                });
            } catch (FailedToReadConfigurationException e) {
                log.warn("Не удалось загрузить конфигурацию стандартных городов и улиц");
            }
        }
    }

    public void saveAll(List<String> streets, City city) {
        streetRepository.saveAll(
                streets.stream().map(name -> {
                    Street street = new Street();
                    street.setName(name);
                    street.setCity(city);
                    street.setDeleted(false);
                    return street;
                }).collect(Collectors.toList())
        );
    }

    public List<Street> getStreetsInCity(Long cityId) {
        return streetRepository.findAllByCity_CityId(cityId);
    }

    public Street getStreetByName(String name) {
        return streetRepository.findByName(name).orElse(null);
    }

    public Street getStreetContainsSubstring(String substring, Long cityId) {
        List<Street> streets = streetRepository.findByNameLikeIgnoreCaseAndCity_CityId("%" + substring + "%", cityId);
        if (streets.size() != 1) return null;
        return streets.get(0);
    }

    public Boolean isExist(Long streetId) {
        return streetRepository.existsById(streetId);
    }

    public Street createIfAbsent(String name, @Nullable String prefix, City city) {
        Street street = streetRepository.findFirstByNameContainingIgnoreCaseAndCity(name, city).orElse(null);
        String trimmedPrefix = prefix != null ? prefix.trim().replaceAll("\\.", "") : null;
        if (street == null) {
            street = streetRepository.save(Street.builder()
                    .name(name)
                    .prefix(trimmedPrefix)
                    .city(city)
                    .deleted(false)
                    .build());
        } else {
            if (!street.getName().equals(name)) {
                street.setName(name);
                street.setPrefix(trimmedPrefix);
                street = streetRepository.save(street);
            } else if (street.getPrefix() == null || !street.getPrefix().equals(trimmedPrefix)) {
                street.setPrefix(trimmedPrefix);
                street = streetRepository.save(street);
            }
        }
        return street;
    }

    public List<Street> containsInName(String[] subQuery) {
        return streetRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String sub : subQuery) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + sub.toLowerCase() + "%"));
                predicates.add(cb.like(cb.lower(root.get("altNames")), "%" + sub.toLowerCase() + "%"));
            }
            return cb.or(predicates.toArray(Predicate[]::new));
        });
    }
}
