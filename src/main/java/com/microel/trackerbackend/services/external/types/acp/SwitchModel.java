package com.microel.trackerbackend.services.external.types.acp;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SwitchModel {
    private Integer id;
    private String name;
    private Short portsCount;
    private Short status;

}
