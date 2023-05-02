package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.entities.address.Address;
import org.springframework.lang.Nullable;

public class AddressMapper {
    @Nullable
    public static AddressDto toDto(@Nullable Address address){
        if(address == null) return null;
        return AddressDto.builder()
                .addressId(address.getAddressId())
                .district(DistrictMapper.toDto(address.getDistrict()))
                .city(CityMapper.toDto(address.getCity()))
                .street(StreetMapper.toDto(address.getStreet()))
                .houseNum(address.getHouseNum())
                .fraction(address.getFraction())
                .build(address.getBuild())
                .entrance(address.getEntrance())
                .floor(address.getFloor())
                .apartmentNum(address.getApartmentNum())
                .apartmentMod(address.getApartmentMod())
                .build();
    }
    @Nullable
    public static Address fromDto(@Nullable AddressDto address) {
        if(address == null) return null;
        return Address.builder()
                .addressId(address.getAddressId())
                .district(DistrictMapper.fromDto(address.getDistrict()))
                .city(CityMapper.fromDto(address.getCity()))
                .street(StreetMapper.fromDto(address.getStreet()))
                .houseNum(address.getHouseNum())
                .fraction(address.getFraction())
                .build(address.getBuild())
                .entrance(address.getEntrance())
                .floor(address.getFloor())
                .apartmentNum(address.getApartmentNum())
                .apartmentMod(address.getApartmentMod())
                .build();
    }
}
