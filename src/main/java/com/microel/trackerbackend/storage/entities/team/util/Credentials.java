package com.microel.trackerbackend.storage.entities.team.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Credentials {
    String getUsername();
    String getPassword();
    void setUsername(String username);
    void setPassword(String password);

    @JsonIgnore
    boolean isNotFull();
}
