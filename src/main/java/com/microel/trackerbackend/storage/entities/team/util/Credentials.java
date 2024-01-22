package com.microel.trackerbackend.storage.entities.team.util;

public interface Credentials {
    String getUsername();
    String getPassword();
    void setUsername(String username);
    void setPassword(String password);

    boolean isNotFull();
}
