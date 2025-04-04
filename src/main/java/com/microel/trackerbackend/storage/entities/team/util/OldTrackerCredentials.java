package com.microel.trackerbackend.storage.entities.team.util;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "old_tracker_credentials")
@ToString
public class OldTrackerCredentials implements Credentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oldTrackerCredentialsId;
    private String username;
    private String password;
    @Nullable
    private String installerId;

    @Override
    public boolean isNotFull() {
        if(username == null || username.isBlank())
            return true;
        if(password == null || password.isBlank())
            return true;
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OldTrackerCredentials that)) return false;
        return Objects.equals(getUsername(), that.getUsername()) && Objects.equals(getPassword(), that.getPassword()) && Objects.equals(getInstallerId(), that.getInstallerId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getPassword(), getInstallerId());
    }
}
