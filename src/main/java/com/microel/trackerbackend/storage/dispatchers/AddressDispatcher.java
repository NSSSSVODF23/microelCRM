package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.misc.CharacterTranslation;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.dto.mapper.AddressMapper;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.address.City;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.address.Street;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.repositories.AddressRepository;
import lombok.*;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
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

    /**
     * Получить {@link AddressLookupRequest} для поиска адресов в БД по поисковой строке.
     * Будет парсить строку регулярными выражениями, пробовать достать части адреса
     *
     * @param query Текстовый запрос
     * @return Список объектов с частями адреса спаршеных из строки текста, если спарсить строку не удалось, возвращает null.
     */
    @Nullable
    public static AddressLookupRequest parseStringQuery(String query) {
        int matchSetting = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

        String specialRegex = "^(?<hn>\\d{1,3})(/(?<hf>\\d{1,3}))?(?<hl>[а-я])?(( с\\.?| стр\\.?| строение|_)(?<hb>\\d{1,3}))?-(?<an>\\d{1,3})";

        Pattern specialPattern = Pattern.compile(specialRegex, matchSetting);
        Matcher specialMatcher = specialPattern.matcher(query);
        AddressLookupRequest specialRequest = AddressLookupRequest.of(specialMatcher);
        if (specialRequest != null)
            return specialRequest;

        List<String> streetParts = List.of(
                "(?<street>\\d{1,2}-?[а-я]{1,2} [а-я]+)",
                "(?<street>\\d{1,2} [а-я]{1,5} [а-я]+)",
                "(?<street>[а-я]+.? ?[а-я]+.? [а-я]+)",
                "(?<street>[а-я]+ \\d-?[а-я]{1,3})",
                "(?<street>\\d{1,2} ?[а-я]{1,5})",
                "(?<street>[а-я]+.? ?[а-я]+)",
                "(?<street>[а-я]+)"
        );
        List<String> houseParts = List.of(
                " (?<hn>\\d{1,4})(/(?<hf>\\d{1,3}))?(?<hl>[а-я])?(( с\\.?| стр\\.?| строение|_)(?<hb>\\d{1,3}))?",
                ""
        );
        List<String> apartParts = List.of(
                "(( |-| кв.)(?<an>\\d{1,3}))?( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                ""
        );

        for (String streetPart : streetParts) {
            for (String housePart : houseParts) {
                for (String apartPart : apartParts) {
                    String rexp = streetPart + housePart + apartPart;
                    Pattern pattern = Pattern.compile(rexp, matchSetting);
                    Matcher matcher = pattern.matcher(query);
                    AddressLookupRequest request = AddressLookupRequest.of(matcher);
                    if (request != null) {
                        return request;
                    }
                }
            }
        }

        return null;
    }

    public List<Long> getAddressIds(FilterModelItem filterModelItem) throws JsonProcessingException {
        if (!(filterModelItem.getValue() instanceof LinkedHashMap addressExample)) return new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Address address = objectMapper.convertValue(addressExample, Address.class);
        List<Address> founded = addressRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (address.getCity() != null)
                predicates.add(cb.equal(root.join("city", JoinType.LEFT).get("cityId"), address.getCity().getCityId()));
            if (address.getStreet() != null)
                predicates.add(cb.equal(root.join("street", JoinType.LEFT).get("streetId"), address.getStreet().getStreetId()));
            if (address.getHouseNum() != null)
                predicates.add(cb.equal(root.get("houseNum"), address.getHouseNum()));
            if (address.getFraction() != null)
                predicates.add(cb.equal(root.get("fraction"), address.getFraction()));
            if (address.getLetter() != null)
                predicates.add(cb.equal(root.get("letter"), address.getLetter()));
            if (address.getBuild() != null) predicates.add(cb.equal(root.get("build"), address.getBuild()));
            if (address.getApartmentNum() != null)
                predicates.add(cb.equal(root.get("apartmentNum"), address.getApartmentNum()));
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        return founded.stream().map(Address::getAddressId).collect(Collectors.toList());
    }

    public List<Address> getAddressInDBByQuery(String stringQuery) {
        List<AddressDto> suggestions = getSuggestions(stringQuery, null, null);
        List<Address> founded = addressRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (AddressDto suggestion : suggestions) {
                List<Predicate> currentAddressPredicates = new ArrayList<>();
                if (suggestion.getCity() != null)
                    currentAddressPredicates.add(cb.equal(root.join("city", JoinType.LEFT).get("cityId"), suggestion.getCity().getCityId()));
                if (suggestion.getStreet() != null)
                    currentAddressPredicates.add(cb.equal(root.join("street", JoinType.LEFT).get("streetId"), suggestion.getStreet().getStreetId()));
                if (suggestion.getHouseNum() != null)
                    currentAddressPredicates.add(cb.equal(root.get("houseNum"), suggestion.getHouseNum()));
                if (suggestion.getFraction() != null)
                    currentAddressPredicates.add(cb.equal(root.get("fraction"), suggestion.getFraction()));
                if (suggestion.getLetter() != null)
                    currentAddressPredicates.add(cb.equal(root.get("letter"), suggestion.getLetter()));
                if (suggestion.getBuild() != null)
                    currentAddressPredicates.add(cb.equal(root.get("build"), suggestion.getBuild()));
                if (suggestion.getApartmentNum() != null)
                    currentAddressPredicates.add(cb.equal(root.get("apartmentNum"), suggestion.getApartmentNum()));

                predicates.add(cb.and(currentAddressPredicates.toArray(Predicate[]::new)));
            }

            query.distinct(true);
            return cb.or(predicates.toArray(Predicate[]::new));
        });

        return founded;
    }

    public Address findIdentical(Address addressData) {
        if (addressData == null) return new Address();
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

                if (addressData.getHouseId() != null)
                    predicates.add(cb.equal(root.get("houseId"), addressData.getHouseId()));
                else predicates.add(cb.isNull(root.get("houseId")));

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

    public List<AddressDto> getSuggestions(String query, @Nullable Boolean isAcpConnected, @Nullable Boolean isHouseOnly) {
        List<Address> suggestions = new ArrayList<>();
        query = CharacterTranslation.translate(query);
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

        int matchSetting = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

        String specialRegex = "^(?<hn>\\d{1,3})(/(?<hf>\\d{1,3}))?(?<hl>[а-я])?(( с\\.?| стр\\.?| строение|_)(?<hb>\\d{1,3}))?-(?<an>\\d{1,3})";

        List<String> cityParts = List.of("^(?<city>[а-я]{3})\\. ([а-я\\-]{2,10}\\.)?", "");
        List<String> streetParts = List.of(
                "(?<street>\\d{1,2}-?[а-я]{1,2} [а-я]+)",
                "(?<street>\\d{1,2} [а-я]{1,5} [а-я]+)",
                "(?<street>[а-я]+.? ?[а-я]+.? [а-я]+)",
                "(?<street>[а-я]+ \\d-?[а-я]{1,3})",
                "(?<street>\\d{1,2} ?[а-я]{1,5})",
                "(?<street>[а-я]+.? ?[а-я]+)",
                "(?<street>[а-я]+)"
        );
        List<String> houseParts = List.of(
                " (?<hn>\\d{1,4})(/(?<hf>\\d{1,3}))?(?<hl>[а-я])?(( с\\.?| стр\\.?| строение|_)(?<hb>\\d{1,3}))?"
        );
        List<String> apartParts = List.of(
                "(( |-| кв.)(?<an>\\d{1,3}))?( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                ""
        );

        Pattern specialPattern = Pattern.compile(specialRegex, matchSetting);
        Matcher specialMatcher = specialPattern.matcher(query);
        AddressLookupRequest specialRequest = AddressLookupRequest.of(specialMatcher);
        if (specialRequest != null) {
            List<House> foundHouses = houseDispatcher.lookupHouses(specialRequest);
            List<Address> collect = foundHouses.stream().filter(house -> !house.isSomeDeleted())
                    .map(house -> house.getAddress(specialRequest.entrance, specialRequest.floor, specialRequest.apartment, specialRequest.apartmentMod)).toList();
            suggestions.addAll(collect);
        }

//        System.out.println("\r\n\r\n----Query '"+query+"' ----");

        for (String cityPart : cityParts) {
            if (!suggestions.isEmpty()) break;
            for (String streetPart : streetParts) {
                for (String housePart : houseParts) {
                    for (String apartPart : apartParts) {
                        String rexp = cityPart + streetPart + housePart + apartPart;
                        Pattern pattern = Pattern.compile(rexp, matchSetting);
                        Matcher matcher = pattern.matcher(query);
                        AddressLookupRequest request = AddressLookupRequest.of(matcher);
                        if (request == null) {
//                            System.out.println("Regex: " + rexp + " failed");
                            continue;
                        }
                        if (request.houseNum != null) {
                            List<House> foundHouses = houseDispatcher.lookup(request, isAcpConnected);
                            List<Address> collect = foundHouses.stream().filter(house -> !house.isSomeDeleted())
                                    .filter(house -> request.apartment == null | (request.apartment != null && house.getIsApartmentHouse()))
                                    .map(house -> {
                                        if (isHouseOnly != null && isHouseOnly) {
                                            return house.getAddress();
                                        }
                                        Address address = house.getAddress(request.entrance, request.floor, request.apartment, request.apartmentMod);
//                                        System.out.println("Regex:  " + rexp + " succeeded");
//                                        System.out.println("Groups:  " + matcher);
//                                        System.out.println("Address: "+ address.getAddressName());
                                        return address;
                                    })
                                    .toList();
                            suggestions.addAll(collect);
                        } else {
                            List<Street> foundStreets = streetDispatcher.containsInName(request.streetName);
                            for (Street street : foundStreets) {
                                List<Address> address = street.getAddress(isAcpConnected);
//                                System.out.println("Regex:  " + rexp + " succeeded");
//                                System.out.println("Groups:  " + matcher);
//                                address.forEach(a-> System.out.println("Address: "+ a.getAddressName()));
                                suggestions.addAll(address);
                            }
                        }
                        if (!suggestions.isEmpty()) break;
                    }
                    if (!suggestions.isEmpty()) break;
                }
                if (!suggestions.isEmpty()) break;
            }
            if (!suggestions.isEmpty()) break;
        }

        if (suggestions.isEmpty()) {
            for (String streetPattern : streetParts) {
                Pattern pattern = Pattern.compile(streetPattern, matchSetting);
                Matcher matcher = pattern.matcher(query);
                AddressLookupRequest request = AddressLookupRequest.of(matcher);
                if (request == null) continue;
                List<Street> foundStreets = streetDispatcher.containsInName(request.streetName);
                for (Street street : foundStreets) {
                    List<Address> address = street.getAddress(isAcpConnected);
//                                System.out.println("Regex:  " + rexp + " succeeded");
//                                System.out.println("Groups:  " + matcher);
//                                address.forEach(a-> System.out.println("Address: "+ a.getAddressName()));
                    suggestions.addAll(address);
                }
            }
        }

        String finalQuery = query;
        return suggestions
                .stream()
                .distinct()
                .sorted(Comparator.comparingInt(o -> levenshteinDistance.apply(finalQuery, o.getAddressName())))
                .map(AddressMapper::toDto)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Address> getSuggestions(Long streetId, String houseQuery, @Nullable Boolean isAcpConnected, @Nullable Boolean isHouseOnly) {
        List<Address> suggestions = new ArrayList<>();
        houseQuery = CharacterTranslation.translate(houseQuery.trim());

        int matchSetting = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

        List<String> houseParts = List.of(
                "(?<hn>\\d{1,4})(/(?<hf>\\d{1,3}))?(?<hl>[а-я])?(( с\\.?| стр\\.?| строение|_)(?<hb>\\d{1,3}))?"
        );
        List<String> apartParts = List.of(
                "(( |-| кв.)(?<an>\\d{1,3}))?( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (п\\.?|под\\.?|подъезд) ?(?<ent>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))?( (э\\.?|эт\\.?|этаж) ?(?<fl>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                "(( |-| кв.)(?<an>\\d{1,3}))( \\((?<am>[а-я]+)\\))?",
                ""
        );

        for (String housePart : houseParts) {
            for (String apartPart : apartParts) {
                String rexp = housePart + apartPart;
                Pattern pattern = Pattern.compile(rexp, matchSetting);
                Matcher matcher = pattern.matcher(houseQuery);
                AddressLookupRequest request = AddressLookupRequest.of(matcher);
                if (request == null) {
                    continue;
                }
                if (request.houseNum != null) {
                    List<House> foundHouses = houseDispatcher.getHouse(streetId, request.getHouseNum(), request.getLetter(), request.getFraction(), request.getBuild(), isAcpConnected);
                    //houseDispatcher.lookup(request, isAcpConnected);
                    List<Address> collect = foundHouses.stream().filter(house -> !house.isSomeDeleted())
                            .filter(house -> request.apartment == null | (request.apartment != null && house.getIsApartmentHouse()))
                            .map(house -> {
                                if (isHouseOnly != null && isHouseOnly) {
                                    return house.getAddress();
                                }
                                return house.getAddress(request.entrance, request.floor, request.apartment, request.apartmentMod);
                            })
                            .toList();
                    suggestions.addAll(collect);
                }
                if (!suggestions.isEmpty()) break;
            }
            if (!suggestions.isEmpty()) break;
        }

        return suggestions;
    }

    public List<House> getSuggestionsHouse(String query, @Nullable Boolean isAcpConnected) {
        query = CharacterTranslation.translate(query);
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

        int matchSetting = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

        List<String> cityParts = List.of("^(?<city>[а-я]{3})\\. ([а-я\\-]{2,10}\\.)?", "");
        List<String> streetParts = List.of(
                "(?<street>\\d{1,2}-?[а-я]{1,2} [а-я]+)",
                "(?<street>\\d{1,2} [а-я]{1,5} [а-я]+)",
                "(?<street>[а-я]+.? ?[а-я]+.? [а-я]+)",
                "(?<street>[а-я]+ \\d-?[а-я]{1,3})",
                "(?<street>\\d{1,2} ?[а-я]{1,5})",
                "(?<street>[а-я]+.? ?[а-я]+)",
                "(?<street>[а-я]+)"
        );
        List<String> houseParts = List.of(
                " (?<hn>\\d{1,4})(/(?<hf>\\d{1,3}))?(?<hl>[а-я])?(( с\\.?| стр\\.?| строение|_)(?<hb>\\d{1,3}))?", ""
        );

        for (String streetPart : streetParts) {
            for (String housePart : houseParts) {
                String rexp = streetPart + housePart;
                Pattern pattern = Pattern.compile(rexp, matchSetting);
                Matcher matcher = pattern.matcher(query);
                AddressLookupRequest request = AddressLookupRequest.of(matcher);
                if (request == null) {
//                            System.out.println("Regex: " + rexp + " failed");
                    continue;
                }
                if (request.houseNum != null) {
                    return houseDispatcher.lookup(request, isAcpConnected);
                } else {
                    List<House> houses = new ArrayList<>();
                    List<Street> foundStreets = streetDispatcher.containsInName(request.streetName);
                    for (Street street : foundStreets) {
                        houses.addAll(street.getHouses().stream().filter(house -> house.getAcpHouseBind() != null).toList());
                    }
                    return houses;
                }
            }
        }

        return new ArrayList<>();
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

    @Nullable
    @Transactional
    public Address convert(String addressString) {
        int matchSetting = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        String regex = "^(?<street>[а-я\\.\\-\\d]+)( [а-я-\\.]{1,6})? (?<hn>\\d{1,4})(/(?<hf>\\d{1,3}))?(?<hl>[а-я])?(_(?<hb>\\d{1,3}))?-?(?<an>\\d{1,3})?";
        Pattern pattern = Pattern.compile(regex, matchSetting);
        Matcher matcher = pattern.matcher(addressString.trim());
        AddressLookupRequest lookupRequest = AddressLookupRequest.of(matcher);
        if (lookupRequest != null) {
            House foundHouse = houseDispatcher.lookupBillingHouse(lookupRequest);
            if (foundHouse != null) {
                return foundHouse.getAddress(lookupRequest.entrance, lookupRequest.floor, lookupRequest.apartment, lookupRequest.apartmentMod);
            }
        }
        return null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class AddressLookupRequest {
        private String cityName;
        private String streetName;
        private Short houseNum;
        @Nullable
        private Short fraction;
        @Nullable
        private Character letter;
        @Nullable
        private Short build;
        @Nullable
        private Short entrance;
        @Nullable
        private Short floor;
        @Nullable
        private Short apartment;
        @Nullable
        private String apartmentMod;

        @Nullable
        public static AddressLookupRequest of(Matcher matcher) {
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
                } catch (IllegalArgumentException | NullPointerException ignored) {
                }
                try {
                    request.setBuild(Short.parseShort(matcher.group("hb")));
                } catch (IllegalArgumentException ignored) {
                }
                try {
                    request.setEntrance(Short.parseShort(matcher.group("ent")));
                } catch (IllegalArgumentException ignored) {
                }
                try {
                    request.setFloor(Short.parseShort(matcher.group("fl")));
                } catch (IllegalArgumentException ignored) {
                }
                try {
                    request.setApartment(Short.parseShort(matcher.group("an")));
                } catch (IllegalArgumentException ignored) {
                }
                try {
                    request.setApartmentMod(matcher.group("am"));
                } catch (IllegalArgumentException ignored) {
                }
                return request;
            }
            return null;
        }

        public String raw() {
            StringBuilder builder = new StringBuilder();
            if (cityName != null) {
                builder.append(cityName);
            }
            if (streetName != null) {
                builder.append(streetName);
            }
            if (houseNum != null) {
                builder.append(houseNum);
            }
            if (fraction != null) {
                builder.append(fraction);
            }
            if (letter != null) {
                builder.append(letter);
            }
            if (build != null) {
                builder.append(build);
            }
            if (entrance != null) {
                builder.append(entrance);
            }
            if (floor != null) {
                builder.append(floor);
            }
            if (apartment != null) {
                builder.append(apartment);
            }
            if (apartmentMod != null) {
                builder.append(apartmentMod);
            }
            return builder.toString();
        }

        public Predicate toPredicate(Join<?, Address> join, CriteriaBuilder cb) {
            List<Predicate> predicates = new ArrayList<>();
//            if (cityName != null) {
//                predicates.add(cb.equal(join.get("city"), cityName));
//            }
            if (streetName != null) {
                Join<Address, Street> streetJoin = join.join("street");
                String lowerCaseStreet = streetName.toLowerCase();
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(streetJoin.get("name")), "%" + lowerCaseStreet + "%"),
                                cb.like(cb.lower(streetJoin.get("billingAlias")), "%" + lowerCaseStreet + "%"),
                                cb.like(cb.lower(streetJoin.get("altNames")), "%" + lowerCaseStreet + "%")
                        )
                );
            }
            if (houseNum != null) {
                predicates.add(cb.equal(join.get("houseNum"), houseNum));
            }
            if (fraction != null) {
                predicates.add(cb.equal(join.get("fraction"), fraction));
            }
            if (letter != null) {
                predicates.add(cb.equal(join.get("letter"), letter));
            }
            if (build != null) {
                predicates.add(cb.equal(join.get("build"), build));
            }
            if (entrance != null) {
                predicates.add(cb.equal(join.get("entrance"), entrance));
            }
            if (floor != null) {
                predicates.add(cb.equal(join.get("floor"), floor));
            }
            if (apartment != null) {
                predicates.add(cb.equal(join.get("apartmentNum"), apartment));
            }
            if (apartmentMod != null) {
                predicates.add(cb.equal(join.get("apartmentMod"), apartmentMod));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }
    }
}
