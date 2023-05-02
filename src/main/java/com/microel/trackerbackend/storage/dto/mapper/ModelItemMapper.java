package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.task.ModelItemDto;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import org.springframework.lang.Nullable;

public class ModelItemMapper {
    @Nullable
    public static ModelItemDto toDto(@Nullable ModelItem modelItem) {
        if (modelItem == null) return null;
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
                .timestampData(modelItem.getTimestampData())
                .phoneData(modelItem.getPhoneData())
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
                .timestampData(modelItem.getTimestampData())
                .phoneData(modelItem.getPhoneData())
                .build();
    }
}
