package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.misc.CharacterTranslation;
import com.microel.trackerbackend.storage.MatchingFactory;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.dto.mapper.AddressMapper;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.repositories.AddressRepository;
import lombok.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AddressDispatcher {
    private final AddressRepository addressRepository;
    private final CityDispatcher cityDispatcher;
    private final StreetDispatcher streetDispatcher;
    private final HouseDispatcher houseDispatcher;

    public AddressDispatcher(AddressRepository addressRepository, CityDispatcher cityDispatcher, StreetDispatcher streetDispatcher, HouseDispatcher houseDispatcher) {
        this.addressRepository = addressRepository;
        this.cityDispatcher = cityDispatcher;
        this.streetDispatcher = streetDispatcher;
        this.houseDispatcher = houseDispatcher;
    }

    @JsonIgnore
    public static List<Address> getAddressVariationsOfCities(List<City> cities) {
        List<Address> variations = new ArrayList<>();
        for (City city : cities) {
            city.getStreets().forEach(street -> {
                street.getHouses().forEach(house -> {
                    Address address = new Address();
                    address.setCity(city);
                    address.setStreet(street);
                    address.setHouse(house);
                    variations.add(address);
                });
            });
        }
        return variations;
    }

    @JsonIgnore
    public static List<Address> getAddressVariationsOfStreets(List<Street> streets) {
        List<Address> variations = new ArrayList<>();
        for (Street street : streets) {
            street.getHouses().forEach(house -> {
                Address address = new Address();
                address.setCity(street.getCity());
                address.setStreet(street);
                address.setHouse(house);
                variations.add(address);
            });
        }
        return variations;
    }

    public List<Long> getAddressIds(FilterModelItem filterModelItem) throws JsonProcessingException {
        Address addressExample = AddressMapper.fromDto(new ObjectMapper().treeToValue(filterModelItem.getValue(), AddressDto.class));
        List<Address> founded = addressRepository.findAll(MatchingFactory.standardExample(addressExample));
        return founded.stream().map(Address::getAddressId).collect(Collectors.toList());
    }

    public Address findIdentical(Address addressData) {
        // Пробуем найти в базе данных адрес с такими же данными как у addressData кроме id
        try {
            return addressRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (addressData.getCity() != null)
                    predicates.add(cb.equal(root.join("city", JoinType.LEFT).get("cityId"), addressData.getCity().getCityId()));
                else predicates.add(cb.isNull(root.join("city")));

                if (addressData.getStreet() != null)
                    predicates.add(cb.equal(root.join("street", JoinType.LEFT).get("streetId"), addressData.getStreet().getStreetId()));
                else predicates.add(cb.isNull(root.join("street")));

                if (addressData.getHouseNum() != null)
                    predicates.add(cb.equal(root.get("houseNum"), addressData.getHouseNum()));
                else predicates.add(cb.isNull(root.get("houseNum")));

                if (addressData.getFraction() != null)
                    predicates.add(cb.equal(root.get("fraction"), addressData.getFraction()));
                else predicates.add(cb.isNull(root.get("fraction")));

                if (addressData.getLetter() != null)
                    predicates.add(cb.equal(root.get("letter"), addressData.getLetter()));
                else predicates.add(cb.isNull(root.get("letter")));

                if (addressData.getBuild() != null) predicates.add(cb.equal(root.get("build"), addressData.getBuild()));
                else predicates.add(cb.isNull(root.get("build")));

                if (addressData.getEntrance() != null)
                    predicates.add(cb.equal(root.get("entrance"), addressData.getEntrance()));
                else predicates.add(cb.isNull(root.get("entrance")));

                if (addressData.getFloor() != null) predicates.add(cb.equal(root.get("floor"), addressData.getFloor()));
                else predicates.add(cb.isNull(root.get("floor")));

                if (addressData.getApartmentNum() != null)
                    predicates.add(cb.equal(root.get("apartmentNum"), addressData.getApartmentNum()));
                else predicates.add(cb.isNull(root.get("apartmentNum")));

                if (addressData.getApartmentMod() != null)
                    predicates.add(cb.equal(root.get("apartmentMod"), addressData.getApartmentMod()));
                else predicates.add(cb.isNull(root.get("apartmentMod")));

                return cb.and(predicates.toArray(Predicate[]::new));
            }).get(0);
        } catch (IndexOutOfBoundsException e) {
            return addressData;
        }
    }

    public List<AddressDto> getSuggestions(String query) {

        int matchSetting = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        List<String> cityParts = List.of("^(?<city>[а-я]+)\\.? ", "^");

        List<String> streetParts = List.of(
                "([а-я\\-]{2,10}\\.)?(?<street>\\d{1,2}\\-?[а-я]? [а-я]+)",
                "([а-я\\-]{2,10}\\.)?(?<street>\\d{1,2}\\-?[а-я]? [а-я]+ [а-я]+)",
                "([а-я\\-]{2,10}\\.)?(?<street>[а-я]+ [а-я]+)",
                "([а-я\\-]{2,10}\\.)?(?<street>[а-я\\.]+)"
        );

        List<String> houseParts = List.of(
                "(?<hn>\\d+)[/\\\\](?<hf>\\d+)(?<hl>[а-я]) (с|стр|строение)\\.?(?<hb>\\d+)",
                "(?<hn>\\d+)[/\\\\](?<hf>\\d+)(?<hl>[а-я])",
                "(?<hn>\\d+)[/\\\\](?<hf>\\d+) (с|стр|строение)\\.?(?<hb>\\d+)",
                "(?<hn>\\d+)[/\\\\](?<hf>\\d+)",
                "(?<hn>\\d+)(?<hl>[а-я]) (с|стр|строение)\\.?(?<hb>\\d+)",
                "(?<hn>\\d+)(?<hl>[а-я])",
                "(?<hn>\\d+) (с|стр|строение)\\.?(?<hb>\\d+)",
                "(?<hn>\\d+)"
        );


        query = CharacterTranslation.translate(query);

        List<Address> filteredSuggestions = new ArrayList<>();
        List<Address> filteredSuggestionsWithoutHouse = new ArrayList<>();

        List<Address> addresses = null;

        for (String cityPart : cityParts) {
            for (String streetPart : streetParts) {
                for (String housePart : houseParts) {
                    Pattern pattern = Pattern.compile(cityPart + streetPart + " " + housePart, matchSetting);
                    Matcher matcher = pattern.matcher(query);
                    addresses = filterSuggestions(matcher);
                    if (addresses != null) {
                        String apartmentQuery = query.substring(matcher.end(matcher.groupCount())).trim();
                        if (!apartmentQuery.isBlank()) {
                            Short apartmentNum = getApartNumOfPattern(apartmentQuery);
                            addresses.forEach(address -> address.setApartmentNum(apartmentNum));
                            if (apartmentNum != null) {
                                String apartmentMod = getApartModOfPattern(apartmentQuery);
                                if (apartmentMod != null && !apartmentMod.isBlank()) {
                                    addresses.forEach(address -> address.setApartmentMod(apartmentMod));
                                }
                            }
                            Short entrance = getEntranceOfPattern(apartmentQuery);
                            addresses.forEach(address -> address.setEntrance(entrance));
                            if (entrance != null) {
                                Short floor = getFloorOfPattern(apartmentQuery);
                                addresses.forEach(address -> address.setFloor(floor));
                            }
                        }
                        filteredSuggestions.addAll(addresses);
                        break;
                    }
                }
                if (addresses == null || addresses.isEmpty()) {
                    Pattern pattern = Pattern.compile(cityPart + streetPart, matchSetting);
                    Matcher matcher = pattern.matcher(query);
                    addresses = filterSuggestions(matcher);
                    if(addresses != null) {
                        filteredSuggestionsWithoutHouse.addAll(addresses);
                    }
                }
            }
        }

        if (filteredSuggestions.isEmpty()) {
            filteredSuggestions = filteredSuggestionsWithoutHouse;
        }

        return filteredSuggestions.stream().sorted(Address::compareTo).map(AddressMapper::toDto).collect(Collectors.toList());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressLookupRequest{
        private String cityName;
        private String streetName;
        private Short houseNum;
        private Short fraction;
        private Character letter;
        private Short build;
    }

    @Nullable
    private Short getApartNumOfPattern(String query) {
        Pattern apartPattern = Pattern.compile("^(к\\.?|кв\\.?|квартира ?)?(?<an>\\d{1,3})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = apartPattern.matcher(query);
        if (matcher.find()) {
            try {
                String apartmentNum = matcher.group("an");
                return Short.parseShort(apartmentNum);
            } catch (IllegalArgumentException ignore) {
            }
        }
        return null;
    }

    @Nullable
    private Short getEntranceOfPattern(String query) {
        Pattern entrancePattern = Pattern.compile("((п\\.?|под\\.?|подъезд ?)(?<ent>\\d{1,2})|(?<entAlt>\\d{1,2})(п\\.?|под\\.?|подъезд))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = entrancePattern.matcher(query);
        if (matcher.find()) {
            try {
                String entrance = matcher.group("ent");
                return Short.parseShort(entrance);
            } catch (IllegalArgumentException ignore) {
            }
            try {
                String entranceAlt = matcher.group("entAlt");
                return Short.parseShort(entranceAlt);
            } catch (IllegalArgumentException ignore) {
            }
        }
        return null;
    }

    @Nullable
    private Short getFloorOfPattern(String query) {
        Pattern floorPattern = Pattern.compile("((э\\.?|эт\\.?|этаж ?)(?<floor>\\d{1,2})|(?<floorAlt>\\d{1,2})(э\\.?|эт\\.?|этаж))", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = floorPattern.matcher(query);
        if (matcher.find()) {
            try {
                String floor = matcher.group("floor");
                return Short.parseShort(floor);
            } catch (IllegalArgumentException ignore) {
            }
            try {
                String floorAlt = matcher.group("floorAlt");
                return Short.parseShort(floorAlt);
            } catch (IllegalArgumentException ignore) {
            }
        }
        return null;
    }

    @Nullable
    private String getApartModOfPattern(String query) {
        Pattern apartModPattern = Pattern.compile("\\(([^\\)]+)\\)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = apartModPattern.matcher(query);
        if (matcher.find()) {
            try {
                return matcher.group(1);
            } catch (IllegalArgumentException ignore) {
            }
        }
        return null;
    }

    @Nullable
    private List<Address> filterSuggestions(Matcher matcher) {
        if (matcher.find()) {
            AddressLookupRequest request = new AddressLookupRequest();
            try {
                request.setCityName(matcher.group("city"));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                request.setStreetName(matcher.group("street"));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                request.setHouseNum(Short.parseShort(matcher.group("hn")));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                request.setFraction(Short.parseShort(matcher.group("hf")));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                request.setLetter(matcher.group("hl").charAt(0));
            } catch (IllegalArgumentException ignored) {
            }
            try {
                request.setBuild(Short.parseShort(matcher.group("hb")));
            } catch (IllegalArgumentException ignored) {
            }
            return houseDispatcher.getExistingAddresses(request);
        }
        return null;
    }
}
