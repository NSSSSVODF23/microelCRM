package com.microel.trackerbackend.modules.transport;

import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DropDownListItem {
    private String label;
    private String value;

    public DropDownListItem(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public static DropDownListItem from(Wireframe wireframe){
        return new DropDownListItem(wireframe.getName(), wireframe.getWireframeId().toString());
    }
}
