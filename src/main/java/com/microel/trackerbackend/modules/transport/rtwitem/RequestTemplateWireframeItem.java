package com.microel.trackerbackend.modules.transport.rtwitem;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestTemplateWireframeItem {
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("dataType")
    private String dataType;
    @JsonProperty("autoTask")
    private Boolean autoTask;
}
