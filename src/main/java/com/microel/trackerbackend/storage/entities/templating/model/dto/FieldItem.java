package com.microel.trackerbackend.storage.entities.templating.model.dto;

import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldItem {
    String name;
    WireframeFieldType type;
    String id;
    String variation;
    Integer listViewIndex;
    Integer orderPosition;
}
