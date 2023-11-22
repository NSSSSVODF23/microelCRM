package com.microel.trackerbackend.storage.dto.address;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
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

    public String getTelegramCallback() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> strMap = new HashMap<>();
        strMap.put("streetBillingAlias", street.getBillingAlias());
        strMap.put("streetName", street.getName());
        if(houseNum != null) strMap.put("houseNum", houseNum.toString());
        if(fraction != null) strMap.put("fraction", fraction.toString());
        if(letter != null) strMap.put("letter", letter.toString());
        if(build != null) strMap.put("build", build.toString());
        if(apartmentNum != null) strMap.put("apartmentNum", apartmentNum.toString());
        return mapper.writeValueAsString(strMap);
    }
}
