package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Building {
    private Integer id;
    private Integer streetId;
    private String houseNum;
    private Short areaId;
    private Short entrances;
    private Short storeys;
    private Short flats;
    private Integer uplinkId;
    private Short hofficeId;
    private String hcommitee;
    private String description;
    private Short status;

}
