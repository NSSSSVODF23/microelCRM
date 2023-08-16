package com.microel.trackerbackend.services.external.types.acp;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Switchport {
    private Integer id;
    private Integer swid;
    private Short port;
    private Short ptype;
    private Short isuplink;
    private String description;

}
