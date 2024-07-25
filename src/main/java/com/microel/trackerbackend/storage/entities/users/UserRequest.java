package com.microel.trackerbackend.storage.entities.users;

import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Entity
@Table(name = "user_request")
public class UserRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRequestId;
    @Enumerated(EnumType.STRING)
    private Type type;
    private String title;
    private String userLogin;
    @Column(columnDefinition = "text")
    private String description;
    private String fromSource;
    @Nullable
    private String chatId;
    @Nullable
    private String phoneNumber;
    private Timestamp created;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "processed_by_employee_intervention_id")
    @Nullable
    private EmployeeIntervention processedBy;
    private Boolean deleted;

    public String getPhoneNumber() {
        if (phoneNumber == null) return null;

        String str = phoneNumber.strip();
        final String regex = "8?(\\d{3})(\\d{3})(\\d{2})(\\d{2})";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return "8 (" + matcher.group(1) + ") " + matcher.group(2) + "-" + matcher.group(3) + "-" + matcher.group(4);
        }
        return phoneNumber;
    }

    public void setType(@Nullable Type type) {
        if (type == null) {
            this.type = Type.UNKNOWN;
        }else {
            this.type = type;
        }
    }

    public static UserRequest of(String userLogin, @Nullable Type type, String title, String description, String fromSource, @Nullable String chatId, @Nullable String phoneNumber) {
        final UserRequest userRequest = new UserRequest();
        userRequest.setUserLogin(userLogin);
        userRequest.setType(type);
        userRequest.setTitle(title);
        userRequest.setDescription(description);
        userRequest.setFromSource(fromSource);
        userRequest.setCreated(Timestamp.from(Instant.now()));
        userRequest.setChatId(chatId);
        userRequest.setPhoneNumber(phoneNumber);
        userRequest.setDeleted(false);
        return userRequest;
    }

    @Getter
    public enum Type {
        REPLACE_TARIFF("REPLACE_TARIFF"),
        APPEND_SERVICE("APPEND_SERVICE"),
        REMOVE_SERVICE("REMOVE_SERVICE"),
        UNKNOWN("UNKNOWN");

        private final String value;

        Type(String value) {
            this.value = value;
        }
    }

    public static class Source {
        public static final String TELEGRAM = "telegram";
        public static final String PHONE = "phone";

        private Source() {
        }
    }
}
