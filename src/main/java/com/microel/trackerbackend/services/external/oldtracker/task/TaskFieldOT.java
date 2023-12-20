package com.microel.trackerbackend.services.external.oldtracker.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public interface TaskFieldOT {
    Integer getId();
    String getName();
    Type getType();

    enum Type {
        TEXT,
        STREET,
        DEFAULT,
        DATE, DATETIME, AD_SOURCE, CONNECTION_TYPE
    }
}
