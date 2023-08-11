package com.microel.trackerbackend.storage.entities.templating.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class StepItem {
    String name;
    List<FieldItem> fields;
    Integer id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StepItem stepItem)) return false;
        return Objects.equals(getName(), stepItem.getName()) && Objects.equals(getFields(), stepItem.getFields()) && Objects.equals(getId(), stepItem.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getFields(), getId());
    }
}
