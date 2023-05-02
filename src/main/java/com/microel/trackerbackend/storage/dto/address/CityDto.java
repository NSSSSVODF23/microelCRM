package com.microel.trackerbackend.storage.dto.address;

import lombok.*;

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
}
