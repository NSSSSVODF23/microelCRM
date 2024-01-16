package com.microel.trackerbackend.storage.dto.address;

import com.microel.trackerbackend.storage.entities.address.District;
import lombok.*;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DistrictDto {
    private Long districtId;
    private String name;
    private Boolean deleted;

    @Nullable
    public static DistrictDto of(@Nullable District district) {
        if(district == null) return null;
        return DistrictDto.builder()
                .districtId(district.getDistrictId())
                .name(district.getName())
                .deleted(district.getDeleted())
                .build();
    }
}
