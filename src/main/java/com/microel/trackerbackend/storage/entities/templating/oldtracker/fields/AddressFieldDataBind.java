package com.microel.trackerbackend.storage.entities.templating.oldtracker.fields;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "AddressFieldDataBind")
public class AddressFieldDataBind extends FieldDataBind {
    private Integer streetFieldId;
    private Integer houseFieldId;
    private Integer apartmentFieldId;
    private Integer entranceFieldId;
    private Integer floorFieldId;
    private Integer backupFieldId;
}
