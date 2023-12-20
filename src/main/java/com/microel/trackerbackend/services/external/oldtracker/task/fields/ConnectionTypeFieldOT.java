package com.microel.trackerbackend.services.external.oldtracker.task.fields;

import com.microel.trackerbackend.services.external.oldtracker.task.TaskFieldOT;
import lombok.Data;
import lombok.NonNull;


@Data
public class ConnectionTypeFieldOT implements TaskFieldOT {
    @NonNull
    private Integer id;
    @NonNull
    private String name;
    private Type type = Type.CONNECTION_TYPE;
}
