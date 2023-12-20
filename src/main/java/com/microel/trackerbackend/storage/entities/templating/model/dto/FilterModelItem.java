package com.microel.trackerbackend.storage.entities.templating.model.dto;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.microel.trackerbackend.storage.entities.templating.ConnectionService;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
public class FilterModelItem {
    @NonNull
    private String id;
    @NonNull
    private WireframeFieldType wireframeFieldType;
    @NonNull
    private String name;
    @Nullable
    private Object value;

    public boolean isNotEmpty(){
        if(value instanceof List valueList && valueList.isEmpty()) return false;
        if(value instanceof String valueString && valueString.isBlank()) return false;
        return value != null;
    }
}
