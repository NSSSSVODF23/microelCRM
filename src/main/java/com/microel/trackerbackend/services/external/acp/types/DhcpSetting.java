package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DhcpSetting {
    private String id;
    private Integer authLeaseTime;
    private Integer leaseTime;
    private Short enableNewBind;
    private String primaryDns;
    private String secondaryDns;
    private Short firstFreeslot;
    private Short lastFreeslot;
    private Short randomFreeslot;
    private String filterDbLog;
    private String voidAuthName;
    private Integer voidAuthExpire;
    private String defaultHostname;
    private String defaultClient;
    private Integer defaultSessionTime;
    private String defaultRemoteId;
    private Integer authExpire;

}
