package com.microel.trackerbackend.storage.dto.address;

import lombok.*;

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

    private Short houseNum;
    private Short fraction;
    private Character letter;
    private Short build;
    private Short entrance;
    private Short floor;

    private Short apartmentNum;
    private String apartmentMod;
}
