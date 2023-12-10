package com.microel.trackerbackend.storage.dto.task;

import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.AcceptingEntry;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.Column;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class WorkLogDto {
    private Long workLogId;
    private TaskDto task;
    private ChatDto chat;
    private Set<EmployeeDto> employees;
    @Nullable
    private String gangLeader;
    private Boolean deferredReport;
    private Set<AcceptingEntry> acceptedEmployees;
    private EmployeeDto creator;
    private Timestamp created;
    private Timestamp closed;
    private Boolean isForceClosed;
    private Set<WorkReportDto> workReports;
    private String forceClosedReason;
    private String targetDescription;
    private WorkLog.Status status;
    private Set<EmployeeDto> whoAccepted;
    private Set<EmployeeDto> whoClosed;
    private String report;
    private Long leadTime;
    private Boolean calculated;
    private List<WorkCalculation> workCalculations;
    private Boolean isReportsUncompleted;
}
