package com.microel.trackerbackend.storage.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.*;

import java.sql.Timestamp;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ModelItemDto {
    private Long modelItemId;
    private String id;
    private String name;
    private WireframeFieldType wireframeFieldType;
    private AddressDto addressData;
    private Boolean booleanData;
    private Float floatData;
    private Integer integerData;
    private String stringData;
    private Timestamp timestampData;
    private Map<String, String> phoneData;
    private String textRepresentation;
    private String textRepresentationForTlg;

    @JsonIgnore
    public Object getValue() {
        switch (wireframeFieldType) {
            case ADDRESS:
                return addressData;
            case BOOLEAN:
                return booleanData;
            case FLOAT:
                return floatData;
            case INTEGER:
                return integerData;
            case LARGE_TEXT:
            case LOGIN:
            case SMALL_TEXT:
            case CONNECTION_SERVICES:
            case EQUIPMENTS:
            case IP:
            case REQUEST_INITIATOR:
            case AD_SOURCE:
                return stringData;
            case PHONE_ARRAY:
                return phoneData;
            default:
                return null;
        }
    }
}
