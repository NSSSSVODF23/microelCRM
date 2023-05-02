package com.microel.trackerbackend.storage.dto.address;

import lombok.*;

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
}
