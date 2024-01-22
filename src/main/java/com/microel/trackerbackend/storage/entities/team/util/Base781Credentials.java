package com.microel.trackerbackend.storage.entities.team.util;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "base_781_credentials")
@ToString
public class Base781Credentials implements Credentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long base781CredentialsId;
    private String username;
    private String password;

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
        if (!(o instanceof Base781Credentials that)) return false;
        return Objects.equals(getBase781CredentialsId(), that.getBase781CredentialsId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBase781CredentialsId());
    }
}
