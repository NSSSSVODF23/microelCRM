package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.address.CityDto;
import com.microel.trackerbackend.storage.entities.address.City;
import org.springframework.lang.Nullable;

public class CityMapper {
    @Nullable
    public static CityDto toDto(@Nullable City city) {
        if (city == null) return null;
        return CityDto.builder()
                .cityId(city.getCityId())
                .name(city.getName())
                .deleted(city.getDeleted())
                .build();
    }

    @Nullable
    public static City fromDto(@Nullable CityDto city) {
        if (city == null) return null;
        return City.builder()
                .cityId(city.getCityId())
                .name(city.getName())
                .deleted(city.getDeleted())
                .build();
    }
}
