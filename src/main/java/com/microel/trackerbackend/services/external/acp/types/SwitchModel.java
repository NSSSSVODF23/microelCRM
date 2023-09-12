package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class SwitchModel {
    private Integer id;
    private String name;
    private Short portsCount;
    private Short status;
}
