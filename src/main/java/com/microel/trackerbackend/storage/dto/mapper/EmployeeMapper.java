package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.team.Employee;
import org.springframework.lang.Nullable;

public class EmployeeMapper {
    @Nullable
    public static EmployeeDto toDto(@Nullable Employee employee) {
        if (employee == null) return null;
        return EmployeeDto.builder()
                .access(employee.getAccess())
                .avatar(employee.getAvatar())
                .created(employee.getCreated())
                .deleted(employee.getDeleted())
                .department(DepartmentMapper.toDto(employee.getDepartment()))
                .firstName(employee.getFirstName())
                .internalPhoneNumber(employee.getInternalPhoneNumber())
                .lastName(employee.getLastName())
                .lastSeen(employee.getLastSeen())
                .login(employee.getLogin())
                .offsite(employee.getOffsite())
                .position(PositionMapper.toDto(employee.getPosition()))
                .secondName(employee.getSecondName())
                .status(employee.getStatus())
                .telegramUserId(employee.getTelegramUserId())
                .build();
    }

    @Nullable
    public static Employee fromDto(@Nullable EmployeeDto employee) {
        if (employee == null) return null;
        return Employee.builder()
                .access(employee.getAccess())
                .avatar(employee.getAvatar())
                .created(employee.getCreated())
                .deleted(employee.getDeleted())
                .department(DepartmentMapper.fromDto(employee.getDepartment()))
                .firstName(employee.getFirstName())
                .internalPhoneNumber(employee.getInternalPhoneNumber())
                .lastName(employee.getLastName())
                .lastSeen(employee.getLastSeen())
                .login(employee.getLogin())
                .offsite(employee.getOffsite())
                .position(PositionMapper.fromDto(employee.getPosition()))
                .secondName(employee.getSecondName())
                .status(employee.getStatus())
                .telegramUserId(employee.getTelegramUserId())
                .build();
    }
}
