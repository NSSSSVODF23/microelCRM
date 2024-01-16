package com.microel.trackerbackend.storage.dto.address;

import com.microel.trackerbackend.storage.entities.address.City;
import lombok.*;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CityDto {
    private Long cityId;
    private String name;
    private Boolean deleted;

    @Nullable
    public static CityDto of(@Nullable City city) {
        if(city == null) return null;
        return CityDto.builder()
                .cityId(city.getCityId())
                .name(city.getName())
                .deleted(city.getDeleted())
                .build();
    }
}
