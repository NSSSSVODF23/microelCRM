package com.microel.trackerbackend.misc;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FactorAction {
    private String name;
    private Float factor;
    private List<UUID> actionUuids;
}
