package com.microel.trackerbackend.services.external.oldtracker.task.fields;

import com.microel.trackerbackend.services.external.oldtracker.task.TaskFieldOT;
import lombok.Data;
import lombok.NonNull;
import org.javatuples.Pair;

import java.util.Map;

@Data
public class StreetFieldOT implements TaskFieldOT {
    @NonNull
    private Integer id;
    @NonNull
    private String name;
    private Type type = Type.STREET;
    @NonNull
    private Map<Long, Pair<Integer, String>> streetsBindings;
    @NonNull
    private Integer defaultNullStreet;
}
