package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.team.DepartmentDto;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import org.springframework.lang.Nullable;

import java.util.HashSet;
import java.util.stream.Collectors;

public class DepartmentMapper {
    @Nullable
    public static DepartmentDto toDto(@Nullable Department department) {
        if (department == null) return null;
        return DepartmentDto.builder()
                .departmentId(department.getDepartmentId())
                .created(department.getCreated())
                .deleted(department.getDeleted())
                .name(department.getName())
                .description(department.getDescription())
                .build();
    }

    @Nullable
    public static Department fromDto(@Nullable DepartmentDto department) {
        if (department == null) return null;
        return Department.builder()
                .departmentId(department.getDepartmentId())
                .created(department.getCreated())
                .deleted(department.getDeleted())
                .name(department.getName())
                .description(department.getDescription())
                .build();
    }
}
