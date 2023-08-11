package com.microel.trackerbackend.services.external.types.acp;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Hoffice {
    private Integer id;
    private String name;
    private String addr;
    private String dispatcher;

}
