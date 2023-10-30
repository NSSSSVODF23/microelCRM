package com.microel.trackerbackend.services.external.acp.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SwitchBaseInfo {
    private Integer id;
    private String name;
    private String ip;
    private Boolean isOnline;
    private Short modelId;
    private String model;
    private Short type;
    private Boolean isHasError;
    private String errorMessage;

    public static SwitchBaseInfo from(Switch sw, String model) {
        return SwitchBaseInfo.builder()
                .id(sw.getId())
                .name(sw.getName())
                .ip(sw.getIpaddr())
                .isOnline(sw.getAdditionalInfo() != null ? sw.getAdditionalInfo().getAvailable() : false)
                .modelId(sw.getSwmodelId())
                .model(model)
                .type(sw.getSwtype())
                .isHasError(sw.getAdditionalInfo() != null ? sw.getAdditionalInfo().getIsLastUpdateError() : false)
                .errorMessage(sw.getAdditionalInfo() != null ? sw.getAdditionalInfo().getLastErrorMessage() : "")
                .build();
    }
}
