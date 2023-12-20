package com.microel.trackerbackend.storage.entities.templating.oldtracker.fields;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "ConnectionTypeFieldDataBind")
public class ConnectionTypeFieldDataBind extends FieldDataBind {
    private String connectionServicesInnerFieldId;
    private Integer ctFieldDataBind;
}
