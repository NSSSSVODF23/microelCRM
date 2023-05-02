package com.microel.trackerbackend.storage.entities.team.util;

public enum EmployeeStatus {
    ONLINE("ONLINE"),
    AWAY("AWAY"),
    OFFLINE("OFFLINE");
    private String status;
    EmployeeStatus(String status) {
        this.status = status;
    }
}
