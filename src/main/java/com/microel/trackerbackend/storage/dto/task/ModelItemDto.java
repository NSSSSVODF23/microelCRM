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
}
