package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OpLog {
    private Integer id;
    private Integer opId;
    private String opLogin;
    private String opIp;
    private String opUa;
    private String action;
    private String url;
    private String postData;
    private String headers;
    private String dateof;
    private String actionObject;

}
