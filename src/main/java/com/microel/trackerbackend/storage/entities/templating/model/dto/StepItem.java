package com.microel.trackerbackend.storage.entities.templating.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StepItem {
    String name;
    List<FieldItem> fields;
    Integer id;
}
