package com.microel.trackerbackend.storage.dto.team;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DepartmentDto {
    private Long departmentId;
    private String name;
    private String description;
    private Boolean deleted;
    private Timestamp created;
    @JsonIgnore
    private Set<EmployeeDto> employees;
}
