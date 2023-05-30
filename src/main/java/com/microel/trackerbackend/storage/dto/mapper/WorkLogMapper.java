package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.task.WorkLogDto;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import org.springframework.lang.Nullable;

import java.util.stream.Collectors;

public class WorkLogMapper {
    @Nullable
    public static WorkLogDto toDto(@Nullable WorkLog workLog){
        if (workLog == null) return null;
        return WorkLogDto.builder()
                .chat(ChatMapper.toDto(workLog.getChat()))
                .closed(workLog.getClosed())
                .created(workLog.getCreated())
                .creator(EmployeeMapper.toDto(workLog.getCreator()))
                .employees(workLog.getEmployees().stream().map(EmployeeMapper::toDto).collect(Collectors.toSet()))
                .acceptedEmployees(workLog.getAcceptedEmployees())
                .isForceClosed(workLog.getIsForceClosed())
                .task(TaskMapper.toDto(workLog.getTask()))
                .workLogId(workLog.getWorkLogId())
                .workReports(workLog.getWorkReports().stream().map(WorkReportMapper::toDto).collect(Collectors.toSet()))
                .build();
    }

    @Nullable
    public static WorkLog fromDto(@Nullable WorkLogDto workLog){
        if (workLog == null) return null;
        return WorkLog.builder()
                .chat(ChatMapper.fromDto(workLog.getChat()))
                .closed(workLog.getClosed())
                .created(workLog.getCreated())
                .creator(EmployeeMapper.fromDto(workLog.getCreator()))
                .employees(workLog.getEmployees().stream().map(EmployeeMapper::fromDto).collect(Collectors.toSet()))
                .acceptedEmployees(workLog.getAcceptedEmployees())
                .isForceClosed(workLog.getIsForceClosed())
                .task(TaskMapper.fromDto(workLog.getTask()))
                .workLogId(workLog.getWorkLogId())
                .workReports(workLog.getWorkReports().stream().map(WorkReportMapper::fromDto).collect(Collectors.toSet()))
                .build();
    }
}
