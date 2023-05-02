package com.microel.trackerbackend.storage.dto.team;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PositionDto {
    private Long positionId;
    private String name;
    private String description;
    private Integer access;
    private Timestamp created;
    private Boolean deleted;
}
