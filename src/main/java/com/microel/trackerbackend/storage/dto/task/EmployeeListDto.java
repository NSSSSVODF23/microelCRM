package com.microel.trackerbackend.storage.dto.task;

import com.microel.trackerbackend.storage.dto.team.DepartmentDto;
import com.microel.trackerbackend.storage.dto.team.PositionDto;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.EmployeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeListDto {
    private String login;
    private String avatar;
    private String secondName;
    private String firstName;
    private String lastName;

    @Nullable
    public static EmployeeListDto of(@Nullable Employee employee) {
        if(employee == null) return null;
        return EmployeeListDto.builder()
                .login(employee.getLogin())
                .avatar(employee.getAvatar())
                .secondName(employee.getSecondName())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();
    }

    public String getFullName(){
        if(firstName != null && lastName != null)
            return firstName + " " + lastName;
        if(firstName != null)
            return firstName;
        return login;
    }
}
