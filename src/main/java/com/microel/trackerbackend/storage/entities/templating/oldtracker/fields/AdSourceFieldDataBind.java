package com.microel.trackerbackend.storage.entities.templating.oldtracker.fields;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "AdSourceFieldDataBind")
public class AdSourceFieldDataBind extends FieldDataBind {
    private Integer adSourceFieldId;
}
