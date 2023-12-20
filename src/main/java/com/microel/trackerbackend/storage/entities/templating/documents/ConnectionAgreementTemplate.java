package com.microel.trackerbackend.storage.entities.templating.documents;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue(value = "ConnectionAgreementTemplate")
public class ConnectionAgreementTemplate extends DocumentTemplate implements Updatable, Named {
    private String loginFieldId;
    private String fullNameFieldId;
    private String dateOfBirthFieldId;
    private String regionOfBirthFieldId;
    private String cityOfBirthFieldId;
    private String passportDetailsFieldId;
    private String addressFieldId;
    private String phoneFieldId;
    private String passwordFieldId;
    private String tariffFieldId;



    @Override
    public void update(Form form) {
        setLoginFieldId(form.getLoginFieldId());
        setFullNameFieldId(form.getFullNameFieldId());
        setDateOfBirthFieldId(form.getDateOfBirthFieldId());
        setRegionOfBirthFieldId(form.getRegionOfBirthFieldId());
        setCityOfBirthFieldId(form.getCityOfBirthFieldId());
        setPassportDetailsFieldId(form.getPassportDetailsFieldId());
        setAddressFieldId(form.getAddressFieldId());
        setPhoneFieldId(form.getPhoneFieldId());
        setPasswordFieldId(form.getPasswordFieldId());
        setTariffFieldId(form.getTariffFieldId());
    }

    @Override
    public String getName() {
        return "Договор на подключение";
    }
}
