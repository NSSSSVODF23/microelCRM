package com.microel.trackerbackend.storage.entities.team.util;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "base_1785_credentials")
@ToString
public class Base1785Credentials implements Credentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long base1785CredentialsId;
    private String username;
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Base1785Credentials that)) return false;
        return Objects.equals(getUsername(), that.getUsername()) && Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getPassword());
    }

    @Override
    public boolean isNotFull() {
        if(username == null || username.isBlank())
            return true;
        if(password == null || password.isBlank())
            return true;
        return false;
    }
}
