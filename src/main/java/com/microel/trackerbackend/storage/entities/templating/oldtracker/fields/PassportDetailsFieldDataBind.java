package com.microel.trackerbackend.storage.entities.templating.oldtracker.fields;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "PassportDetailsFieldDataBind")
public class PassportDetailsFieldDataBind extends FieldDataBind {
    private Integer passportSeriesFieldId;
    private Integer passportNumberFieldId;
    private Integer passportIssuedByFieldId;
    private Integer passportIssuedDateFieldId;
    private Integer registrationAddressFieldId;
}
