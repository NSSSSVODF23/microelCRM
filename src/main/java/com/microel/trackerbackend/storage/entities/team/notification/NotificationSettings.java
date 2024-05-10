package com.microel.trackerbackend.storage.entities.team.notification;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "notification_settings")
public class NotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationSettingsId;
    private Boolean muted;
    private String passedTypes;

    public static NotificationSettings from(Form form) {
        final NotificationSettings settings = new NotificationSettings();
        settings.setMuted(form.muted != null && form.muted);
        settings.setPassedTypes(form.passedTypes == null? List.of() : form.passedTypes);
        return settings;
    }

    public List<NotificationType> getPassedTypes() {
        return Stream.of(passedTypes.split(","))
                .map(NotificationType::fromString)
                .toList();
    }

    public void setPassedTypes(List<NotificationType> passedTypes) {
        this.passedTypes = passedTypes.stream()
                .map(NotificationType::getValue)
                .collect(Collectors.joining(","));
    }

    public void update(Form form) {
        setMuted(form.muted != null && form.muted);
        setPassedTypes(form.passedTypes == null? List.of() : form.passedTypes);
    }

    @Data
    public static class Form {
        private Boolean muted;
        private List<NotificationType> passedTypes;
    }
}
