package com.microel.trackerbackend.storage.dto.address;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HouseDto {
    private Long houseId;
    private Short houseNum;
    private Short fraction;
    private Character letter;
    private Short build;
    private StreetDto street;
}
