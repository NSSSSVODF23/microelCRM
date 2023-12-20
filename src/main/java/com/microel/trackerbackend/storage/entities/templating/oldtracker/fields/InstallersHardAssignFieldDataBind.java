package com.microel.trackerbackend.storage.entities.templating.oldtracker.fields;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "InstallersHardAssignFieldDataBind")
public class InstallersHardAssignFieldDataBind extends FieldDataBind {
    private Integer hardAssignTimeFieldId;
    private Integer hardAssignNamesFieldId;
}
