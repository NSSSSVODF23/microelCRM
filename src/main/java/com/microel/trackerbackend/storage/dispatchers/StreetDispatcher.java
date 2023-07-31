package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.DefaultCitiesConf;
import com.microel.trackerbackend.misc.CharacterTranslation;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.StreetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
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
    private final StompController stompController;

    public StreetDispatcher(StreetRepository streetRepository, CityDispatcher cityDispatcher, ConfigurationStorage configurationStorage, StompController stompController) {
        this.streetRepository = streetRepository;
        this.cityDispatcher = cityDispatcher;
        this.configurationStorage = configurationStorage;
        this.stompController = stompController;
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
        return streetRepository.findAllByCity_CityIdAndDeletedIsFalseOrderByName(cityId);
    }

    public Street getStreetByName(String name) {
        return streetRepository.findByName(name).orElse(null);
    }

    public Street getStreetContainsSubstring(String substring, Long cityId) {
        List<Street> streets = streetRepository.findByNameLikeIgnoreCaseAndCity_CityIdOrderByName("%" + substring + "%", cityId);
        if (streets.size() != 1) return null;
        return streets.get(0);
    }

    public List<Street> getStreetsContainsSubstring(String substring, Long cityId) {
        return streetRepository.findByNameLikeIgnoreCaseAndCity_CityIdOrderByName("%" + substring + "%", cityId);
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
    public List<Street> containsInName(String subQuery) {
        return streetRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + subQuery.toLowerCase() + "%"));
            predicates.add(cb.like(cb.lower(root.get("billingAlias")), "%" + subQuery.toLowerCase() + "%"));
            predicates.add(cb.like(cb.lower(root.get("altNames")), "%" + subQuery.toLowerCase() + "%"));
            return cb.or(predicates.toArray(Predicate[]::new));
        });
    }
    public List<Street> containsInName(String subQuery, Short houseNum, @Nullable Character letter, @Nullable Short fraction, @Nullable Short build) {
        return streetRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + subQuery.toLowerCase() + "%"));
            predicates.add(cb.like(cb.lower(root.get("billingAlias")), "%" + subQuery.toLowerCase() + "%"));
            predicates.add(cb.like(cb.lower(root.get("altNames")), "%" + subQuery.toLowerCase() + "%"));
            Join<Street, House> houses = root.join("houses", JoinType.LEFT);
            List<Predicate> housePreds = new ArrayList<>();
            housePreds.add(cb.equal(houses.get("houseNum"), houseNum));
                    cb.equal(houses.get("houseNum"), houseNum);
            if (letter != null) {
                predicates.add(cb.equal(houses.get("letter"), letter));
            }
            if (fraction != null) {
                predicates.add(cb.equal(houses.get("fraction"), fraction));
            }
            if (build != null) {
                predicates.add(cb.equal(houses.get("build"), build));
            }
            return cb.and(cb.or(predicates.toArray(Predicate[]::new)), cb.and(housePreds.toArray(Predicate[]::new)));
        });
    }

    public List<Street> lookupByFilter(String filter, Long cityId) {
        String prepared = CharacterTranslation.translate(filter).toLowerCase();
        return streetRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Street,City> city = root.join("city", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + prepared + "%"),
                    cb.like(cb.lower(root.get("altNames")), "%" + prepared + "%"),
                    cb.like(cb.lower(root.get("billingAlias")), "%" + prepared + "%")
            ));
            predicates.add(cb.equal(city.get("cityId"), cityId));
            predicates.add(cb.equal(root.get("deleted"), false));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Order.asc("name")));
    }

    public Street create(Long cityId, Street.Form form) {
        if(!form.isValid()) throw new IllegalFields("Данные для создания улицы не валидны");
        City city = cityDispatcher.getCity(cityId);
        if(city.getDeleted()) throw new IllegalFields("Невозможно добавить улицу в город который был удален");
        boolean exists = streetRepository.existsByNameAndDeletedFalse(form.getName());
        if(exists) throw new IllegalFields("Улица с таким названием уже существует");
        Street street = Street.builder()
                .name(form.getName())
                .prefix(form.getPrefix())
                .city(city)
                .billingAlias(form.getBillingAlias())
                .deleted(false)
                .build();
        if(form.getAltNames() != null) street.setAltNames(String.join(",", form.getAltNames()));
        Street save = streetRepository.save(street);
        stompController.createStreet(save);
        return save;
    }

    public Street edit(Long streetId, Street.Form form) {
        Street street = streetRepository.findById(streetId).orElseThrow(() -> new IllegalFields("Улица не найдена"));
        if(!form.isValid()) throw new IllegalFields("Данные для редактирования улицы не валидны");
        street.setName(form.getName());
        street.setPrefix(form.getPrefix());
        street.setBillingAlias(form.getBillingAlias());
        if(form.getAltNames() != null) street.setAltNames(String.join(",", form.getAltNames()));
        Street save = streetRepository.save(street);
        stompController.updateStreet(save);
        return street;
    }

    public Street delete(Long streetId) {
        Street street = streetRepository.findById(streetId).orElseThrow(() -> new IllegalFields("Улица не найдена"));
        street.setDeleted(true);
        Street save = streetRepository.save(street);
        stompController.deleteStreet(save);
        return save;
    }

    public Street getById(Long streetId) {
        return streetRepository.findById(streetId).orElseThrow(() -> new IllegalFields("Улица не найдена"));
    }
}
