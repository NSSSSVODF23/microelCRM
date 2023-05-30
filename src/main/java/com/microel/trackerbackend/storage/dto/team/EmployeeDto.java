package com.microel.trackerbackend.storage.dto.team;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.team.util.EmployeeStatus;
import lombok.*;

import java.sql.Timestamp;
import java.util.Objects;

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

    public String getFullName(){
        if(firstName != null && lastName != null)
            return firstName + " " + lastName;
        if(firstName != null)
            return firstName;
        return login;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeDto)) return false;
        EmployeeDto that = (EmployeeDto) o;
        return Objects.equals(getLogin(), that.getLogin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLogin());
    }
}
