package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.task.ModelItemDto;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import org.springframework.lang.Nullable;

import java.util.HashMap;

public class ModelItemMapper {
    @Nullable
    public static ModelItemDto toDto(@Nullable ModelItem modelItem) {
        if (modelItem == null) return null;
        HashMap<String, String> phoneMap = modelItem.getPhoneData().entrySet().stream().reduce(new HashMap<String, String>(), (map, entry) -> {
            map.put(entry.getKey(), entry.getValue());
            return map;
        }, (map1, map2) -> {
            map1.putAll(map2);
            return map1;
        });
        return ModelItemDto.builder()
                .modelItemId(modelItem.getModelItemId())
                .name(modelItem.getName())
                .id(modelItem.getId())
                .booleanData(modelItem.getBooleanData())
                .integerData(modelItem.getIntegerData())
                .floatData(modelItem.getFloatData())
                .stringData(modelItem.getStringData())
                .addressData(AddressMapper.toDto(modelItem.getAddressData()))
                .wireframeFieldType(modelItem.getWireframeFieldType())
                .variation(modelItem.getVariation())
                .timestampData(modelItem.getTimestampData())
                .phoneData(phoneMap)
                .connectionServicesData(modelItem.getConnectionServicesData())
                .textRepresentation(modelItem.getTextRepresentation())
                .textRepresentationForTlg(modelItem.getTextRepresentationForTlg())
                .build();
    }

    @Nullable
    public static ModelItem fromDto(@Nullable ModelItemDto modelItem) {
        if (modelItem == null) return null;
        return ModelItem.builder()
                .modelItemId(modelItem.getModelItemId())
                .name(modelItem.getName())
                .id(modelItem.getId())
                .booleanData(modelItem.getBooleanData())
                .integerData(modelItem.getIntegerData())
                .floatData(modelItem.getFloatData())
                .stringData(modelItem.getStringData())
                .addressData(AddressMapper.fromDto(modelItem.getAddressData()))
                .wireframeFieldType(modelItem.getWireframeFieldType())
                .variation(modelItem.getVariation())
                .timestampData(modelItem.getTimestampData())
                .phoneData(modelItem.getPhoneData())
                .connectionServicesData(modelItem.getConnectionServicesData())
                .build();
    }
}
