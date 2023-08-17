package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NatRule {
    private Integer id;
    private String internalip;
    private String externalip;
    private Integer serverid;
    private Integer natPort;
    private Integer shapeClassid;
    private Integer shapePrio;
    private Integer shapeIn;
    private Integer shapeOut;

}
