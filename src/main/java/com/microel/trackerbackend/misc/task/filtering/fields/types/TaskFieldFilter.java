package com.microel.trackerbackend.misc.task.filtering.fields.types;

import com.microel.trackerbackend.modules.transport.ip.IP;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.templating.AdvertisingSource;
import com.microel.trackerbackend.storage.entities.templating.ConnectionType;
import com.microel.trackerbackend.storage.entities.templating.DataConnectionService;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.analysis.function.Add;
import org.springframework.lang.Nullable;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TaskFieldFilter{
    private FilteringType type;
    @Nullable
    private String textValue;
    @Nullable
    private Address addressValue;
    @Nullable
    private AdvertisingSource adSourceValue;
    @Nullable
    private ConnectionType connectionTypeValue;
    @Nullable
    private Long connectionServiceValue;

    public boolean isEmpty() {
        switch (type){
            case TEXT, LOGIN, PHONE -> {
                if(textValue == null || textValue.isBlank()) return true;
            }
            case ADDRESS -> {
                if(addressValue == null) return true;
            }
            case CONNECTION_TYPE -> {
                if(connectionTypeValue == null) return true;
            }
            case CONNECTION_SERVICE -> {
                if(connectionServiceValue == null) return true;
            }
            case AD_SOURCE -> {
                if(adSourceValue == null) return true;
            }
        }
        return false;
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    @Nullable
    public Predicate toPredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        switch (type) {
            case TEXT -> {
                return textPredicate(fieldsJoin, cb);
            }
            case LOGIN -> {
                return loginPredicate(fieldsJoin, cb);
            }
            case ADDRESS -> {
                return addressPredicate(fieldsJoin, cb);
            }
            case PHONE -> {
                return phonePredicate(fieldsJoin, cb);
            }
            case CONNECTION_TYPE -> {
                return connectionTypePredicate(fieldsJoin, cb);
            }
            case CONNECTION_SERVICE -> {
                return connectionServicePredicate(fieldsJoin, cb);
            }
            case AD_SOURCE -> {
                return adSourcePredicate(fieldsJoin, cb);
            }
        }
        return null;
    }

    @Nullable
    private Predicate textPredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        if(textValue == null || textValue.isBlank()) return null;
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(fieldsJoin.get("wireframeFieldType")
                .in(WireframeFieldType.SMALL_TEXT, WireframeFieldType.LARGE_TEXT, WireframeFieldType.IP, WireframeFieldType.REQUEST_INITIATOR));
        predicates.add(cb.like(cb.lower(fieldsJoin.get("stringData")), "%" + textValue.toLowerCase() + "%"));

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    @Nullable
    private Predicate loginPredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        if(textValue == null || textValue.isBlank()) return null;
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(fieldsJoin.get("wireframeFieldType").in(WireframeFieldType.LOGIN));
        predicates.add(cb.like(cb.lower(fieldsJoin.get("stringData")), "%" + textValue.toLowerCase() + "%"));

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    @Nullable
    private Predicate addressPredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        if(addressValue == null) return null;
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(fieldsJoin.get("wireframeFieldType").in(WireframeFieldType.ADDRESS));

        Join<ModelItem, Address> addressJoin = fieldsJoin.join("addressData", JoinType.LEFT);
        Join<Address, City> cityJoin = addressJoin.join("city", JoinType.LEFT);
        Join<Address, Street> streetJoin = addressJoin.join("street", JoinType.LEFT);

        if(addressValue.getCity() != null)
            predicates.add(cb.equal(cityJoin.get("cityId"), addressValue.getCity().getCityId()));
        if(addressValue.getStreet() != null)
            predicates.add(cb.equal(streetJoin.get("streetId"), addressValue.getStreet().getStreetId()));
        if(addressValue.getHouseNum() != null)
            predicates.add(cb.equal(addressJoin.get("houseNum"), addressValue.getHouseNum()));
        if(addressValue.getFraction() != null)
            predicates.add(cb.equal(addressJoin.get("fraction"), addressValue.getFraction()));
        if(addressValue.getLetter() != null)
            predicates.add(cb.equal(addressJoin.get("letter"), addressValue.getLetter()));
        if(addressValue.getBuild() != null)
            predicates.add(cb.equal(addressJoin.get("build"), addressValue.getBuild()));
        if(addressValue.getApartmentNum() != null)
            predicates.add(cb.equal(addressJoin.get("apartmentNum"), addressValue.getApartmentNum()));

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    @Nullable
    private Predicate adSourcePredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        if(adSourceValue == null) return null;
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(fieldsJoin.get("wireframeFieldType").in(WireframeFieldType.AD_SOURCE));
        predicates.add(cb.equal(fieldsJoin.get("stringData"), adSourceValue.getValue()));

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    @Nullable
    private Predicate connectionTypePredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        if(connectionTypeValue == null) return null;
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(fieldsJoin.get("wireframeFieldType").in(WireframeFieldType.CONNECTION_TYPE));
        predicates.add(cb.equal(fieldsJoin.get("stringData"), connectionTypeValue.getValue()));

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    @Nullable
    private Predicate connectionServicePredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        if(connectionServiceValue == null) return null;
        List<Predicate> predicates = new ArrayList<>();
        Join<ModelItem, DataConnectionService> connectionServiceJoin = fieldsJoin.join("connectionServicesData");

        predicates.add(fieldsJoin.get("wireframeFieldType").in(WireframeFieldType.CONNECTION_SERVICES));
        predicates.add(cb.equal(connectionServiceJoin.get("connectionService"), connectionServiceValue));

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    @Nullable
    private Predicate phonePredicate(Root<ModelItem> fieldsJoin, CriteriaBuilder cb) {
        if(textValue == null || textValue.isBlank()) return null;
        List<Predicate> predicates = new ArrayList<>();

        Join<ModelItem, Map<String, String>> phoneJoin = fieldsJoin.join("phoneData", JoinType.LEFT);

        predicates.add(fieldsJoin.get("wireframeFieldType").in(WireframeFieldType.PHONE_ARRAY));

        String preparedString = "%" + textValue.replaceAll("\\D", "") + "%";
        predicates.add(
            cb.like(
                cb.function(
                    "REGEXP_REPLACE",
                    String.class, phoneJoin,
                    cb.literal("\\D"),
                    cb.literal(""),
                    cb.literal('g')),
                preparedString)
        );

        return cb.and(predicates.toArray(Predicate[]::new));
    }
}
