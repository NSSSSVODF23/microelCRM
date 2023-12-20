package com.microel.trackerbackend.storage.entities.templating.oldtracker.fields;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "FullNameFieldDataBind")
public class FullNameFieldDataBind extends FieldDataBind {
    private Integer lastNameFieldId;
    private Integer firstNameFieldId;
    private Integer patronymicFieldId;
}
