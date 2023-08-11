package com.microel.trackerbackend.storage.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.entities.templating.DataConnectionService;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;
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
    private String variation;
    private AddressDto addressData;
    private Boolean booleanData;
    private Float floatData;
    private Integer integerData;
    private String stringData;
    private Timestamp timestampData;
    private Map<String, String> phoneData;
    private List<DataConnectionService> connectionServicesData;
    private String textRepresentation;
    private String textRepresentationForTlg;

    @JsonIgnore
    public Object getValue() {
        return switch (wireframeFieldType) {
            case ADDRESS -> addressData;
            case BOOLEAN -> booleanData;
            case FLOAT -> floatData;
            case INTEGER -> integerData;
            case LARGE_TEXT, LOGIN, SMALL_TEXT, EQUIPMENTS, IP, REQUEST_INITIATOR, AD_SOURCE, CONNECTION_TYPE ->
                    stringData;
            case PHONE_ARRAY -> phoneData;
            case CONNECTION_SERVICES -> connectionServicesData;
        };
    }
}
