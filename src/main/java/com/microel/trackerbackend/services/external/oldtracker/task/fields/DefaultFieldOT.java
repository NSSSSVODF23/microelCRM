package com.microel.trackerbackend.services.external.oldtracker.task.fields;

import com.microel.trackerbackend.services.external.oldtracker.task.TaskFieldOT;
import lombok.Data;
import lombok.NonNull;


@Data
public class DefaultFieldOT implements TaskFieldOT {
    @NonNull
    private Integer id;
    @NonNull
    private String name;
    @NonNull
    private String value;
    private Type type = Type.DEFAULT;
}
