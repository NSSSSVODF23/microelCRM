package com.microel.trackerbackend.services.api.external.acp.types;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Street {
    private Integer id;
    private String name;
    private Short status;

}
