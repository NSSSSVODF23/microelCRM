package com.microel.trackerbackend.storage.dto.task;

import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class WorkReportDto {
    private Long workReportId;
    private String description;
    private EmployeeDto author;
    private Timestamp created;
    private Boolean awaitingWriting;
}
