package com.microel.trackerbackend.storage.dto.address;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class StreetDto {
    private Long streetId;
    private String name;
    private CityDto city;
    private Boolean deleted;
}
