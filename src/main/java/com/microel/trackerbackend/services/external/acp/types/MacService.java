package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MacService {
    private Integer id;
    private Short type;
    private String macaddr;
    private String device;
    private String owner;

}
