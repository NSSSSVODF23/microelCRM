package com.microel.trackerbackend.parsers.addresses;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class StreetResponse {
    private Boolean actual;
    private String aoGuid;
    private Integer aoLevel;
    private String areaCode;
    private String autoCode;
    private String cityCode;
    private String classType;
    private String ctarCode;
    private String extrCode;
    private String formalName;
    private String guid;
    private Boolean isAddedManually;
    private String offName;
    private String parentGuid;
    private String placeCode;
    private String regionCode;
    private String sextCode;
    private String shortName;
    private String streetCode;
}
