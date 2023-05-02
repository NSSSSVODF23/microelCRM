package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.storage.MatchingFactory;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.repositories.AddressRepository;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AddressDispatcher {
    private final AddressRepository addressRepository;

    public AddressDispatcher(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Long> getAddressIds(FilterModelItem filterModelItem) throws JsonProcessingException {
        Address addressExample = new ObjectMapper().treeToValue(filterModelItem.getValue(), Address.class);
        List<Address> founded = addressRepository.findAll(MatchingFactory.standardExample(addressExample));
        return founded.stream().map(Address::getAddressId).collect(Collectors.toList());
    }

    public Address findIdentical(Address addressData) {
        // Пробуем найти в базе данных адрес с такими же данными как у addressData кроме id
        try {
            return addressRepository.findAll( (root, query, cb)->{
                List<Predicate> predicates = new ArrayList<>();
                if(addressData.getCity() !=null)
                    predicates.add(cb.equal(root.join("city", JoinType.LEFT).get("cityId"), addressData.getCity().getCityId()));
                else
                    predicates.add(cb.isNull(root.join("city")));

                if(addressData.getStreet() !=null)
                    predicates.add(cb.equal(root.join("street", JoinType.LEFT).get("streetId"), addressData.getStreet().getStreetId()));
                else
                    predicates.add(cb.isNull(root.join("street")));

                if(addressData.getHouseNum() != null)
                    predicates.add(cb.equal(root.get("houseNum"), addressData.getHouseNum()));
                else
                    predicates.add(cb.isNull(root.get("houseNum")));

                if(addressData.getFraction() !=null)
                    predicates.add(cb.equal(root.get("fraction"), addressData.getFraction()));
                else
                    predicates.add(cb.isNull(root.get("fraction")));

                if(addressData.getLetter() !=null)
                    predicates.add(cb.equal(root.get("letter"), addressData.getLetter()));
                else
                    predicates.add(cb.isNull(root.get("letter")));

                if(addressData.getBuild() !=null)
                    predicates.add(cb.equal(root.get("build"), addressData.getBuild()));
                else
                    predicates.add(cb.isNull(root.get("build")));

                if(addressData.getEntrance() !=null)
                    predicates.add(cb.equal(root.get("entrance"), addressData.getEntrance()));
                else
                    predicates.add(cb.isNull(root.get("entrance")));

                if(addressData.getFloor() !=null)
                    predicates.add(cb.equal(root.get("floor"), addressData.getFloor()));
                else
                    predicates.add(cb.isNull(root.get("floor")));

                if(addressData.getApartmentNum() !=null)
                    predicates.add(cb.equal(root.get("apartmentNum"), addressData.getApartmentNum()));
                else
                    predicates.add(cb.isNull(root.get("apartmentNum")));

                if(addressData.getApartmentMod() !=null)
                    predicates.add(cb.equal(root.get("apartmentMod"), addressData.getApartmentMod()));
                else
                    predicates.add(cb.isNull(root.get("apartmentMod")));

                return cb.and(predicates.toArray(Predicate[]::new));
            }).get(0);
        }catch (IndexOutOfBoundsException e){
            return addressData;
        }
    }
}
