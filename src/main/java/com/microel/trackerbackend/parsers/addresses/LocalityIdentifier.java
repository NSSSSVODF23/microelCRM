package com.microel.trackerbackend.parsers.addresses;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LocalityIdentifier {
    private final String regionCode = "f10763dc-63e3-48db-83e1-9c566fe3092b";
    private String areaCode = "";
    private String cityCode = "";
    private String settlementCode = "";
}
