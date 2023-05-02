package com.microel.trackerbackend.storage.dto.team;

import com.microel.trackerbackend.storage.entities.team.util.EmployeeStatus;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class EmployeeDto {
    private String login;
    private DepartmentDto department;
    private PositionDto position;
    private String avatar;
    private String secondName;
    private String firstName;
    private String lastName;
    private String internalPhoneNumber;
    private Integer access;
    private Timestamp created;
    private String telegramUserId;
    private Boolean offsite;
    private Boolean deleted;
    private EmployeeStatus status;
    private Timestamp lastSeen;
}
