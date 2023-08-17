package com.microel.trackerbackend.services.external.acp.types;

import lombok.*;

import java.time.Instant;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LogBuilding {
    private Integer id;
    private Integer buildingsId;
    private Instant logDate;
    private Short logType;
    private String memo;
    private String author;
    private Short status;

}
