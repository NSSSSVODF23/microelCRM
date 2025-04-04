package com.microel.trackerbackend.storage.entities.team;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microel.trackerbackend.storage.entities.team.notification.NotificationSettings;
import com.microel.trackerbackend.storage.entities.team.util.*;
import com.microel.trackerbackend.storage.entities.templating.DefaultObserver;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import javax.persistence.*;import javax.persistence.CascadeType;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "employees")
public class Employee implements Observer{
    @Id
    private String login;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_department_id")
    private Department department;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "а_position_id")
    private Position position;
    @Column(length = 64)
    private String avatar;
    @Column(length = 32)
    private String secondName;
    @Column(length = 32)
    private String firstName;
    @Column(length = 32)
    private String lastName;
    private String internalPhoneNumber;
    private Integer access;
    @JsonIgnore
    private String password;
    private Timestamp created;
    private String telegramUserId;
    @Nullable
    private String telegramGroupChatId;
    @Column(columnDefinition = "boolean default false")
    private Boolean offsite;
    private Boolean deleted;
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;
    private Timestamp lastSeen;
    @Nullable
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_employee_id")
    private PhyPhoneInfo phyPhoneInfo;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "f_old_tracker_credentials_id")
    private OldTrackerCredentials oldTrackerCredentials;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "f_base_781_credentials_id")
    private Base781Credentials base781Credentials;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "f_base_1785_credentials_id")
    private Base1785Credentials base1785Credentials;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "f_base_1783_credentials_id")
    private Base1783Credentials base1783Credentials;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "telegram_options_id")
    @JsonIgnore
    private TelegramOptions telegramOptions;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "f_notification_settings_id")
    private NotificationSettings notificationSettings;


    public void setTelegramOptions(TelegramOptions telegramOptions){
        telegramOptions.setEmployee(this);
        this.telegramOptions = telegramOptions;
    }

    @JsonIgnore
    public boolean isHasOldTrackerCredentials(){
        return oldTrackerCredentials != null
                && oldTrackerCredentials.getUsername() != null
                && !oldTrackerCredentials.getUsername().isBlank()
                && oldTrackerCredentials.getPassword() != null
                && !oldTrackerCredentials.getPassword().isBlank();
    }

    public static Employee getSystem() {
        return Employee.builder()
                .login("system")
                .firstName("Система")
                .deleted(false)
                .build();
    }

    public String getFullName(){
        if(firstName != null && lastName != null)
            return firstName + " " + lastName;
        if(firstName != null)
            return firstName;
        return login;
    }

    public boolean isHasNotGroup(){
        return telegramGroupChatId == null || telegramGroupChatId.isBlank();
    }

    public boolean isHasGroup(){
        return !isHasNotGroup();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee employee = (Employee) o;
        return Objects.equals(getLogin(), employee.getLogin());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Employee{");
        sb.append("login='").append(login).append('\'');
        sb.append(", secondName='").append(secondName).append('\'');
        sb.append(", firstName='").append(firstName).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLogin());
    }

    @Override
    @JsonIgnore
    public String getIdentifier() {
        return getLogin();
    }

    @Override
    @JsonIgnore
    public String getDesignation() {
        return "@" + getLogin();
    }

    public static Employee from(DefaultObserver defaultObserver){
        return Employee.builder().login(defaultObserver.getTargetId()).build();
    }
}
