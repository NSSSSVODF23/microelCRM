package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.HouseRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HouseDispatcher {
    private final HouseRepository houseRepository;
    private final StreetDispatcher streetDispatcher;
    private final StompController stompController;

    public HouseDispatcher(HouseRepository houseRepository, StreetDispatcher streetDispatcher, StompController stompController) {
        this.houseRepository = houseRepository;
        this.streetDispatcher = streetDispatcher;
        this.stompController = stompController;
    }

    public House createIfAbsent(House parsedHouse, Street streetEntity) {
        House houseEntity = houseRepository.findFirstByHouseNumAndFractionAndLetterAndBuildAndStreet(parsedHouse.getHouseNum(),
                parsedHouse.getFraction(), parsedHouse.getLetter(), parsedHouse.getBuild(), streetEntity).orElse(null);
        if (houseEntity == null) {
            parsedHouse.setStreet(streetEntity);
            houseEntity = houseRepository.save(parsedHouse);
        }
        return houseEntity;
    }

    public List<House> lookup(AddressDispatcher.AddressLookupRequest request, @Nullable Boolean isAcpConnected) {
        return houseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicatesStreet = new ArrayList<>();
            List<Predicate> predicatesHouse = new ArrayList<>();
            Join<House, Street> streetJoin = root.join("street", JoinType.LEFT);
            Join<Street, City> cityJoin = streetJoin.join("city", JoinType.LEFT);
            if (request.getStreetName() != null) {
                predicatesStreet.add(cb.like(cb.lower(streetJoin.get("name")), "%" + request.getStreetName().toLowerCase() + "%"));
                predicatesStreet.add(cb.like(cb.lower(streetJoin.get("billingAlias")), "%" + request.getStreetName().toLowerCase() + "%"));
                predicatesStreet.add(cb.like(cb.lower(streetJoin.get("altNames")), "%" + request.getStreetName().toLowerCase() + "%"));
            }

//            if(request.getCityName() != null){
//                predicatesHouse.add(cb.like(cb.lower(cityJoin.get("name")), "%"+request.getCityName()+"%"));
//            }

            predicatesHouse.add(cb.equal(root.get("houseNum"), request.getHouseNum()));
            if (request.getFraction() != null) {
                predicatesHouse.add(cb.equal(root.get("fraction"), request.getFraction()));
            }
            if (request.getLetter() != null) {
                predicatesHouse.add(cb.equal(root.get("letter"), request.getLetter()));
            }
            if (request.getBuild() != null) {
                predicatesHouse.add(cb.equal(root.get("build"), request.getBuild()));
            }
            if(isAcpConnected != null){
                if(isAcpConnected) predicatesHouse.add(cb.isNotNull(root.get("acpHouseBind")));
                else predicatesHouse.add(cb.isNull(root.get("acpHouseBind")));
            }
            return cb.and(cb.or(predicatesStreet.toArray(Predicate[]::new)), cb.and(predicatesHouse.toArray(Predicate[]::new)));
        });
    }

    public House lookupBillingHouse(AddressDispatcher.AddressLookupRequest request) {
        return houseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<House, Street> streetJoin = root.join("street", JoinType.LEFT);

            List<Predicate> streetNamesPredicates = new ArrayList<>();

            streetNamesPredicates.add(cb.equal(cb.lower(streetJoin.get("billingAlias")), request.getStreetName().toLowerCase()));
            streetNamesPredicates.add(cb.equal(cb.lower(streetJoin.get("name")), request.getStreetName().toLowerCase()));


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
            if (request.getApartment() != null){
                predicates.add(cb.isTrue(root.get("isApartmentHouse")));
            }
            return cb.and(cb.or(streetNamesPredicates.toArray(Predicate[]::new)), cb.and(predicates.toArray(Predicate[]::new)));
        }).stream().findFirst().orElse(null);
    }

    public List<House> lookupHouses(AddressDispatcher.AddressLookupRequest request) {
        return houseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicatesHouse = new ArrayList<>();

            predicatesHouse.add(cb.equal(root.get("houseNum"), request.getHouseNum()));
            if (request.getFraction() != null) {
                predicatesHouse.add(cb.equal(root.get("fraction"), request.getFraction()));
            }
            if (request.getLetter() != null) {
                predicatesHouse.add(cb.equal(root.get("letter"), request.getLetter()));
            }
            if (request.getBuild() != null) {
                predicatesHouse.add(cb.equal(root.get("build"), request.getBuild()));
            }

            return cb.and(predicatesHouse.toArray(Predicate[]::new));
        });
    }

    public List<Address> getExistingAddresses(AddressDispatcher.AddressLookupRequest request) {
        if (request.getCityName() == null && request.getStreetName() == null) {
            return new ArrayList<>();
        }
        return houseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> streetJoin = root.join("street", JoinType.LEFT);
            Join<Object, Object> cityJoin = streetJoin.join("city", JoinType.LEFT);
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

    public List<House> getByStreetId(Long streetId) {
        return houseRepository.findByStreet_StreetIdAndDeletedFalseOrderByHouseNum(streetId);
    }

    public boolean isHouseExistsByForm(Street street, House.Form form) {
        return isHouseExistsByForm(street, form, null);
    }

    public boolean isHouseExistsByForm(Street street, House.Form form, @Nullable Long id) {
        return houseRepository.exists((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("houseNum"), form.getHouseNum()));

            if (form.getLetter() != null)
                predicates.add(cb.equal(root.get("letter"), form.getLetter()));
            else
                predicates.add(cb.isNull(root.get("letter")));

            if (form.getFraction() != null)
                predicates.add(cb.equal(root.get("fraction"), form.getFraction()));
            else
                predicates.add(cb.isNull(root.get("fraction")));

            if (form.getBuild() != null)
                predicates.add(cb.equal(root.get("build"), form.getBuild()));
            else
                predicates.add(cb.isNull(root.get("build")));

            predicates.add(cb.equal(root.get("street"), street));
            predicates.add(cb.equal(root.get("deleted"), false));
            if(id != null) predicates.add(cb.notEqual(root.get("houseId"), id));

            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public House create(Long streetId, House.Form form) {
        if (!form.isValid()) throw new IllegalFields("Данные для создания дома не валидны");
        Street street = streetDispatcher.getById(streetId);
        if (street.getDeleted()) throw new IllegalFields("Улица удалена");
        boolean exists = isHouseExistsByForm(street, form);
        if (exists) throw new AlreadyExists("Дом с таким номером уже существует");
        House house = House.builder()
                .houseNum(form.getHouseNum())
                .fraction(form.getFraction())
                .letter(form.getLetter())
                .build(form.getBuild())
                .street(street)
                .place(form.getPlace() == null ? null : form.getPlace().toPlace())
                .isApartmentHouse(form.getIsApartmentHouse())
                .acpHouseBind(form.getAcpHouseBind())
                .deleted(false)
                .build();
        House save = houseRepository.save(house);
        stompController.createHouse(save);
        return save;
    }

    public House edit(Long id, House.Form form) {
        House house = houseRepository.findById(id).orElseThrow(() -> new EntryNotFound("Дом не найден"));
        if (!form.isValid()) throw new IllegalFields("Данные для обновления дома не валидны");
        Street street = house.getStreet();
        boolean exists = isHouseExistsByForm(street, form, id);
        if (exists) throw new AlreadyExists("Дом с таким номером уже существует");
        if (form.isFullEqual(house)) throw new IllegalFields("Данные не изменены");
        house.setHouseNum(form.getHouseNum());
        house.setFraction(form.getFraction());
        house.setLetter(form.getLetter());
        house.setBuild(form.getBuild());
        house.setPlace(form.getPlace() == null ? null : form.getPlace().toPlace());
        house.setIsApartmentHouse(form.getIsApartmentHouse());
        house.setAcpHouseBind(form.getAcpHouseBind());
        House save = houseRepository.save(house);
        stompController.updateHouse(save);
        return save;
    }

    public House delete(Long id) {
        House house = houseRepository.findById(id).orElseThrow(() -> new EntryNotFound("Дом не найден"));
        house.setDeleted(true);
        House save = houseRepository.save(house);
        stompController.deleteHouse(save);
        return save;
    }

    @Nullable
    public House getByAcpBindId(Integer buildId) {
        return houseRepository.findTopByAcpHouseBind_BuildingIdAndDeletedFalse(buildId);
    }

    @Transactional
    public House get(Long id) {
        return houseRepository.findById(id).orElseThrow(() -> new EntryNotFound("Дом не найден"));
    }
}
