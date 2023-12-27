package com.microel.trackerbackend.services.external.acp.types;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class DhcpSessions {
    private Long id;
    private String authName;
    private String macaddr;
    private String ipaddr;
    private Timestamp evTime;
    private String evType;
    private String description;
}
