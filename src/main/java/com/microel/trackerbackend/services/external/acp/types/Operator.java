package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Operator {
    private Integer id;
    private String login;
    private String passw;
    private Integer privilage;
    private String email;
    private String lastIp;
    private Instant lastLog;

}
