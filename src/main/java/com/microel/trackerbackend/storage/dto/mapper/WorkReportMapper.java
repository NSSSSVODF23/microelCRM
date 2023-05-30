package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.task.WorkReportDto;
import com.microel.trackerbackend.storage.entities.task.WorkReport;
import org.springframework.lang.Nullable;

public class WorkReportMapper {
    @Nullable
    public static WorkReportDto toDto(@Nullable WorkReport workReport){
        if(workReport == null) return null;
        return WorkReportDto.builder()
                .author(EmployeeMapper.toDto(workReport.getAuthor()))
                .created(workReport.getCreated())
                .description(workReport.getDescription())
                .workReportId(workReport.getWorkReportId())
                .build();
    }

    @Nullable
    public static WorkReport fromDto(@Nullable WorkReportDto workReport){
        if(workReport == null) return null;
        return WorkReport.builder()
                .author(EmployeeMapper.fromDto(workReport.getAuthor()))
                .created(workReport.getCreated())
                .description(workReport.getDescription())
                .workReportId(workReport.getWorkReportId())
                .build();
    }
}
