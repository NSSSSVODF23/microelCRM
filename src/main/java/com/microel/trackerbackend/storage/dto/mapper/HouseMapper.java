package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.address.HouseDto;
import com.microel.trackerbackend.storage.entities.address.House;
import org.springframework.lang.Nullable;

public class HouseMapper {
    @Nullable
    public static HouseDto toDto(@Nullable House house){
        if(house == null) return null;
        return HouseDto.builder()
                .houseId(house.getHouseId())
                .houseNum(house.getHouseNum())
                .fraction(house.getFraction())
                .letter(house.getLetter())
                .build(house.getBuild())
                .street(StreetMapper.toDto(house.getStreet()))
                .build();
    }

    @Nullable
    public static House fromDto(@Nullable HouseDto houseDto){
        if(houseDto == null) return null;
        return House.builder()
                .houseId(houseDto.getHouseId())
                .houseNum(houseDto.getHouseNum())
                .fraction(houseDto.getFraction())
                .letter(houseDto.getLetter())
                .build(houseDto.getBuild())
                .street(StreetMapper.fromDto(houseDto.getStreet()))
                .build();
    }
}
