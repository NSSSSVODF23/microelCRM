package com.microel.trackerbackend.services.external.types.acp;

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
