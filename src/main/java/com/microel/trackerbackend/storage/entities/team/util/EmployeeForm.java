package com.microel.trackerbackend.storage.entities.team.util;

import com.microel.trackerbackend.security.PasswordService;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
public class EmployeeForm {
    private String lastName;
    private String firstName;
    private String secondName;
    private String internalPhoneNumber;
    private Integer access;
    private String login;
    private String password;
    private String telegramUserId;
    @Nullable
    private String telegramGroupChatId;
    private Long department;
    private Long position;
    @Nullable
    private Boolean offsite;
    @Nullable
    private OldTrackerCredentials oldTrackerCredentials;
    @Nullable
    private Base781Credentials base781Credentials;
    @Nullable
    private Base1785Credentials base1785Credentials;
    @Nullable
    private Base1783Credentials base1783Credentials;

    public Employee toNewEmployee(Department department, Position position, PasswordService passwordService){
        Employee.EmployeeBuilder employeeBuilder = Employee.builder();
        employeeBuilder
                .firstName(firstName)
                .lastName(lastName)
                .secondName(secondName)
                .login(login)
                .password(passwordService.encryptPassword(password))
                .access(access)
                .internalPhoneNumber(internalPhoneNumber)
                .telegramUserId(telegramUserId)
                .department(department)
                .position(position)
                .created(Timestamp.from(Instant.now()))
                .offsite(offsite != null && offsite)
                .telegramGroupChatId((offsite != null && offsite) ? telegramGroupChatId : null)
                .oldTrackerCredentials(oldTrackerCredentials)
                .base781Credentials(base781Credentials)
                .base1785Credentials(base1785Credentials)
                .base1783Credentials(base1783Credentials)
                .deleted(false);

        return employeeBuilder.build();
    }

    public Employee updateEmployee(Employee employee, Department department, Position position, PasswordService passwordService) {
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setSecondName(secondName);
        if (!password.equals("password")) employee.setPassword(passwordService.encryptPassword(password));
        employee.setAccess(access);
        employee.setInternalPhoneNumber(internalPhoneNumber);
        employee.setTelegramUserId(telegramUserId);
        employee.setDepartment(department);
        employee.setPosition(position);

        employee.setOffsite(offsite != null && offsite);
        employee.setTelegramGroupChatId((offsite != null && offsite) ? telegramGroupChatId : null);

        if(!Objects.equals(employee.getOldTrackerCredentials(), oldTrackerCredentials))
            employee.setOldTrackerCredentials(oldTrackerCredentials);
        if(!Objects.equals(employee.getBase781Credentials(), base781Credentials))
            employee.setBase781Credentials(base781Credentials);
        if(!Objects.equals(employee.getBase1785Credentials(), base1785Credentials))
            employee.setBase1785Credentials(base1785Credentials);
        if(!Objects.equals(employee.getBase1783Credentials(), base1783Credentials))
            employee.setBase1783Credentials(base1783Credentials);

        return employee;
    }
}
