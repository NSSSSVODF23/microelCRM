package com.microel.trackerbackend.storage.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.address.AddressDto;
import com.microel.trackerbackend.storage.entities.equipment.ClientEquipmentRealization;
import com.microel.trackerbackend.storage.entities.templating.DataConnectionService;
import com.microel.trackerbackend.storage.entities.templating.PassportDetails;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
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
    private FieldItem.DisplayType displayType;
    private AddressDto addressData;
    private Boolean booleanData;
    private Float floatData;
    private Integer integerData;
    private String stringData;
    private Timestamp timestampData;
    private Map<String, String> phoneData;
    private List<DataConnectionService> connectionServicesData;
    private List<ClientEquipmentRealization> equipmentRealizationsData;
    private PassportDetails passportDetailsData;
    private String textRepresentation;
    private String textRepresentationForTlg;


    // todo Для добавления типа поля, нужно добавить сюда2
    @JsonIgnore
    public Object getValue() {
        return switch (wireframeFieldType) {
            case ADDRESS -> addressData;
            case BOOLEAN -> booleanData;
            case FLOAT -> floatData;
            case INTEGER -> integerData;
            case COUNTING_LIVES, LARGE_TEXT, LOGIN, SMALL_TEXT, IP, REQUEST_INITIATOR, AD_SOURCE, CONNECTION_TYPE -> stringData;
            case PHONE_ARRAY -> phoneData;
            case CONNECTION_SERVICES -> connectionServicesData;
            case EQUIPMENTS -> equipmentRealizationsData;
            case PASSPORT_DETAILS -> passportDetailsData;
        };
    }
}
