package com.microel.trackerbackend.storage.entities.team.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

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
    private Boolean offsite;
}
