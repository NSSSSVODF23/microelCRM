package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.address.DistrictDto;
import com.microel.trackerbackend.storage.entities.address.District;
import org.springframework.lang.Nullable;

public class DistrictMapper {
    @Nullable
    public static DistrictDto toDto(@Nullable District district) {
        if (district == null) return null;
        return DistrictDto.builder()
                .districtId(district.getDistrictId())
                .name(district.getName())
                .deleted(district.getDeleted())
                .build();
    }

    @Nullable
    public static District fromDto(@Nullable DistrictDto district) {
        if (district == null) return null;
        return District.builder()
                .districtId(district.getDistrictId())
                .name(district.getName())
                .deleted(district.getDeleted())
                .build();
    }
}
