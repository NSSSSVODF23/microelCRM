package com.microel.trackerbackend.storage.entities.users;

import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

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
    private String userLogin;
    private String description;
    private String fromSource;
    private Timestamp created;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "processed_by_employee_intervention_id")
    @Nullable
    private EmployeeIntervention processedBy;
    private Boolean deleted;

    public static UserRequest of(String userLogin, Type type, String description, String fromSource) {
        final UserRequest userRequest = new UserRequest();
        userRequest.setUserLogin(userLogin);
        userRequest.setType(type);
        userRequest.setDescription(description);
        userRequest.setFromSource(fromSource);
        userRequest.setCreated(Timestamp.from(Instant.now()));
        userRequest.setDeleted(false);
        return userRequest;
    }

    public String getSubject() {
        return type.getLabel();
    }

    @Getter
    public enum Type {
        REPLACE_TARIFF("REPLACE_TARIFF"),
        APPEND_SERVICE("APPEND_SERVICE"),
        REMOVE_SERVICE("REMOVE_SERVICE");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getLabel() {
            return switch (this) {
                case APPEND_SERVICE -> "Подключение услуги";
                case REMOVE_SERVICE -> "Удаление услуги";
                case REPLACE_TARIFF -> "Изменение тарифа";
            };
        }
    }
}
