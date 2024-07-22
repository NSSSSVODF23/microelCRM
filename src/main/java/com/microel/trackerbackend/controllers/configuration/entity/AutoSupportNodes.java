package com.microel.trackerbackend.controllers.configuration.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.confstore.AbstractConfiguration;
import com.microel.trackerbackend.misc.autosupport.schema.Node;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutoSupportNodes implements AbstractConfiguration {
    public static final AutoSupportNodes DEFAULT = new AutoSupportNodes();
    private Node defaultNodes;

    @Override
    public String fileName() {
        return "auto_support_nodes.json";
    }

    @Override
    @JsonIgnore
    public Boolean isFilled() {
        return true;
    }

    @Override
    public String toString() {
        return "";
    }
}
