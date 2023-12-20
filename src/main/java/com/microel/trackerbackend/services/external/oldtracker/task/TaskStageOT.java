package com.microel.trackerbackend.services.external.oldtracker.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TaskStageOT {
    private Integer id;
    private String name;
    private Type type;

    public enum Type{
        ARCHIVE,
        ACTIVE;
    }
}
