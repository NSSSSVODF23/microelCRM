package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.address.StreetDto;
import com.microel.trackerbackend.storage.entities.address.Street;
import org.springframework.lang.Nullable;

public class StreetMapper {
    @Nullable
    public static StreetDto toDto(@Nullable Street street) {
        if (street == null) return null;
        return StreetDto.builder()
                .streetId(street.getStreetId())
                .city(CityMapper.toDto(street.getCity()))
                .deleted(street.getDeleted())
                .name(street.getName())
                .build();
    }

    @Nullable
    public static Street fromDto(@Nullable StreetDto street) {
        if (street == null) return null;
        return Street.builder()
                .streetId(street.getStreetId())
                .city(CityMapper.fromDto(street.getCity()))
                .deleted(street.getDeleted())
                .name(street.getName())
                .build();
    }
}
