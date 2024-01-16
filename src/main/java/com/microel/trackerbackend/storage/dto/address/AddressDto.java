package com.microel.trackerbackend.storage.dto.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import com.microel.trackerbackend.storage.entities.address.Address;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressDto {
    private Long addressId;
    private CityDto city;
    private DistrictDto district;
    private StreetDto street;

    private Long houseId;
    private Short houseNum;
    private Short fraction;
    private Character letter;
    private Short build;
    private Short entrance;
    private Short floor;

    private Short apartmentNum;
    private String apartmentMod;

    private String addressName;

    private AcpHouse acpHouseBind;
    private String streetNamePart;

    private String tailPart;

    public String getTelegramCallback() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> strMap = new HashMap<>();
        if(street != null) strMap.put("streetBillingAlias", street.getBillingAlias());
        if(street != null) strMap.put("streetName", street.getName());
        if(houseNum != null) strMap.put("houseNum", houseNum.toString());
        if(fraction != null) strMap.put("fraction", fraction.toString());
        if(letter != null) strMap.put("letter", letter.toString());
        if(build != null) strMap.put("build", build.toString());
        if(apartmentNum != null) strMap.put("apartmentNum", apartmentNum.toString());
        return mapper.writeValueAsString(strMap);
    }

    @Nullable
    public static AddressDto of(@Nullable Address address){
        if(address == null) return null;
        return AddressDto.builder()
                .addressId(address.getAddressId())
                .city(CityDto.of(address.getCity()))
                .district(DistrictDto.of(address.getDistrict()))
                .street(StreetDto.of(address.getStreet()))
                .houseId(address.getHouseId())
                .houseNum(address.getHouseNum())
                .fraction(address.getFraction())
                .letter(address.getLetter())
                .build(address.getBuild())
                .entrance(address.getEntrance())
                .floor(address.getFloor())
                .apartmentNum(address.getApartmentNum())
                .apartmentMod(address.getApartmentMod())
                .addressName(address.getAddressName())
                .acpHouseBind(address.getAcpHouseBind())
                .streetNamePart(address.getStreetNamePart())
                .tailPart(address.getTailPart())
                .build();
    }
}
