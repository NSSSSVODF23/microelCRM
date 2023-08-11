package com.microel.trackerbackend.services.external.types.acp;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Switch {
    private Integer id;
    private String name;
    private Short swtype;
    private Short swmodelId;
    private String ipaddr;
    private Short protocol;
    private String login;
    private String passw;
    private Integer buildId;
    private Short entrance;
    private Short storey;
    private Integer phyUplinkId;
    private String description;
    private Short status;
    private Short enableMonitor;
    private Short enableSms;
    private Short enableBackup;

}
