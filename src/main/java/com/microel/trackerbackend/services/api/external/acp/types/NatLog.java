package com.microel.trackerbackend.services.api.external.acp.types;

import lombok.*;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NatLog {
    private Integer id;
    private String login;
    private Instant natUptime;
    private Instant natDowntime;
    private Instant natSestime;
    private String natInternalip;
    private String natExternalip;

}
