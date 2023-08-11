package com.microel.trackerbackend.misc;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class FactorAction {
    private String name;
    private Float factor;
    private List<UUID> actionUuids;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FactorAction that)) return false;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getFactor(), that.getFactor()) && Objects.equals(getActionUuids(), that.getActionUuids());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getFactor(), getActionUuids());
    }
}
