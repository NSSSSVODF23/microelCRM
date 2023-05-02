package com.microel.trackerbackend.storage.entities.templating.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilterModelItem {
    private String id;
    private WireframeFieldType wireframeFieldType;
    private JsonNode value;
}
