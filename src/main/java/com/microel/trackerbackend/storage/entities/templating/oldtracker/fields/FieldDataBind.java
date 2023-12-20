package com.microel.trackerbackend.storage.entities.templating.oldtracker.fields;

import com.microel.trackerbackend.services.api.ResponseException;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;


@Getter
@Setter
@Entity
@Table(name = "field_data_binds")
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue(value = "FieldDataBind")
public class FieldDataBind {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fieldDataBindId;
    @Nullable
    private String fieldItemId;
    @Column(insertable = false, updatable = false)
    private String type;

    @Data
    public static class Form{
        private String type;
        @Nullable
        private Long fieldDataBindId;
        private String fieldItemId;
        @Nullable
        private Integer streetFieldId;
        @Nullable
        private Integer houseFieldId;
        @Nullable
        private Integer apartmentFieldId;
        @Nullable
        private Integer entranceFieldId;
        @Nullable
        private Integer floorFieldId;
        @Nullable
        private Integer adSourceFieldId;
        @Nullable
        private String connectionServicesInnerFieldId;
        @Nullable
        private Integer ctFieldDataBind;
        @Nullable
        private Integer dateFieldDataBind;
        @Nullable
        private Integer dateTimeFieldDataBind;
        @Nullable
        private Integer defaultFieldId;
        @Nullable
        private Integer hardAssignTimeFieldId;
        @Nullable
        private Integer hardAssignNamesFieldId;
        @Nullable
        private Integer simpleAssignFieldId;
        @Nullable
        private Integer textFieldId;
        @Nullable
        private Integer lastNameFieldId;
        @Nullable
        private Integer firstNameFieldId;
        @Nullable
        private Integer patronymicFieldId;
        @Nullable
        private Integer backupFieldId;
        @Nullable
        private Integer passportSeriesFieldId;
        @Nullable
        private Integer passportNumberFieldId;
        @Nullable
        private Integer passportIssuedByFieldId;
        @Nullable
        private Integer passportIssuedDateFieldId;
        @Nullable
        private Integer registrationAddressFieldId;

        public FieldDataBind toEntity() {
            switch (getType()){
                case "AddressFieldDataBind"->{
                    AddressFieldDataBind addressFieldDataBind = new AddressFieldDataBind();
                    addressFieldDataBind.setType(getType());
                    addressFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    addressFieldDataBind.setFieldItemId(getFieldItemId());
                    addressFieldDataBind.setStreetFieldId(getStreetFieldId());
                    addressFieldDataBind.setHouseFieldId(getHouseFieldId());
                    addressFieldDataBind.setApartmentFieldId(getApartmentFieldId());
                    addressFieldDataBind.setEntranceFieldId(getEntranceFieldId());
                    addressFieldDataBind.setFloorFieldId(getFloorFieldId());
                    addressFieldDataBind.setBackupFieldId(getBackupFieldId());
                    return addressFieldDataBind;
                }
                case "AdSourceFieldDataBind" -> {
                    AdSourceFieldDataBind adSourceFieldDataBind = new AdSourceFieldDataBind();
                    adSourceFieldDataBind.setType(getType());
                    adSourceFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    adSourceFieldDataBind.setFieldItemId(getFieldItemId());
                    adSourceFieldDataBind.setAdSourceFieldId(getAdSourceFieldId());
                    return adSourceFieldDataBind;
                }
                case "ConnectionTypeFieldDataBind" -> {
                    ConnectionTypeFieldDataBind connectionTypeFieldDataBind = new ConnectionTypeFieldDataBind();
                    connectionTypeFieldDataBind.setType(getType());
                    connectionTypeFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    connectionTypeFieldDataBind.setFieldItemId(getFieldItemId());
                    connectionTypeFieldDataBind.setConnectionServicesInnerFieldId(getConnectionServicesInnerFieldId());
                    connectionTypeFieldDataBind.setCtFieldDataBind(getCtFieldDataBind());
                    return connectionTypeFieldDataBind;
                }
                case "DateFieldDataBind" -> {
                    DateFieldDataBind dateFieldDataBind = new DateFieldDataBind();
                    dateFieldDataBind.setType(getType());
                    dateFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    dateFieldDataBind.setFieldItemId(getFieldItemId());
                    dateFieldDataBind.setDateFieldDataBind(getDateFieldDataBind());
                    return dateFieldDataBind;
                }
                case "DateTimeFieldDataBind" -> {
                    DateTimeFieldDataBind dateTimeFieldDataBind = new DateTimeFieldDataBind();
                    dateTimeFieldDataBind.setType(getType());
                    dateTimeFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    dateTimeFieldDataBind.setFieldItemId(getFieldItemId());
                    dateTimeFieldDataBind.setDateTimeFieldDataBind(getDateTimeFieldDataBind());
                    return dateTimeFieldDataBind;
                }
                case "DefaultFieldDataBind" -> {
                    DefaultFieldDataBind defaultFieldDataBind = new DefaultFieldDataBind();
                    defaultFieldDataBind.setType(getType());
                    defaultFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    defaultFieldDataBind.setFieldItemId(getFieldItemId());
                    defaultFieldDataBind.setDefaultFieldId(getDefaultFieldId());
                    return defaultFieldDataBind;
                }
                case "InstallersHardAssignFieldDataBind" -> {
                    InstallersHardAssignFieldDataBind installersHardAssignFieldDataBind = new InstallersHardAssignFieldDataBind();
                    installersHardAssignFieldDataBind.setType(getType());
                    installersHardAssignFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    installersHardAssignFieldDataBind.setFieldItemId(getFieldItemId());
                    installersHardAssignFieldDataBind.setHardAssignTimeFieldId(getHardAssignTimeFieldId());
                    installersHardAssignFieldDataBind.setHardAssignNamesFieldId(getHardAssignNamesFieldId());
                    return installersHardAssignFieldDataBind;
                }
                case "InstallersSimpleAssignFieldDataBind" -> {
                    InstallersSimpleAssignFieldDataBind installersSimpleAssignFieldDataBind = new InstallersSimpleAssignFieldDataBind();
                    installersSimpleAssignFieldDataBind.setType(getType());
                    installersSimpleAssignFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    installersSimpleAssignFieldDataBind.setFieldItemId(getFieldItemId());
                    installersSimpleAssignFieldDataBind.setSimpleAssignFieldId(getSimpleAssignFieldId());
                    return installersSimpleAssignFieldDataBind;
                }
                case "TextFieldDataBind" -> {
                    TextFieldDataBind textFieldDataBind = new TextFieldDataBind();
                    textFieldDataBind.setType(getType());
                    textFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    textFieldDataBind.setFieldItemId(getFieldItemId());
                    textFieldDataBind.setTextFieldId(getTextFieldId());
                    return textFieldDataBind;
                }
                case "FullNameFieldDataBind" -> {
                    FullNameFieldDataBind fullNameFieldDataBind = new FullNameFieldDataBind();
                    fullNameFieldDataBind.setType(getType());
                    fullNameFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    fullNameFieldDataBind.setFieldItemId(getFieldItemId());
                    fullNameFieldDataBind.setLastNameFieldId(getLastNameFieldId());
                    fullNameFieldDataBind.setFirstNameFieldId(getFirstNameFieldId());
                    fullNameFieldDataBind.setPatronymicFieldId(getPatronymicFieldId());
                    return fullNameFieldDataBind;
                }
                case "PassportDetailsFieldDataBind"->{
                    PassportDetailsFieldDataBind passportDetailsFieldDataBind = new PassportDetailsFieldDataBind();
                    passportDetailsFieldDataBind.setType(getType());
                    passportDetailsFieldDataBind.setFieldDataBindId(getFieldDataBindId());
                    passportDetailsFieldDataBind.setFieldItemId(getFieldItemId());
                    passportDetailsFieldDataBind.setPassportSeriesFieldId(getPassportSeriesFieldId());
                    passportDetailsFieldDataBind.setPassportNumberFieldId(getPassportNumberFieldId());
                    passportDetailsFieldDataBind.setPassportIssuedByFieldId(getPassportIssuedByFieldId());
                    passportDetailsFieldDataBind.setPassportIssuedDateFieldId(getPassportIssuedDateFieldId());
                    passportDetailsFieldDataBind.setRegistrationAddressFieldId(getRegistrationAddressFieldId());
                    return passportDetailsFieldDataBind;
                }
                default -> throw new ResponseException("Неизвестный тип поля: " + getType());
            }
        }
    }
}
