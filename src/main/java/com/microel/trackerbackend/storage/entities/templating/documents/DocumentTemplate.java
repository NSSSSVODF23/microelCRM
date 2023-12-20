package com.microel.trackerbackend.storage.entities.templating.documents;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.fields.FieldDataBind;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.CascadeType;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "document_templates")
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue(value = "DocumentTemplate")
public class DocumentTemplate implements Updatable, Named {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentTemplateId;
    @Column(insertable = false, updatable = false)
    private String type;

    @JsonIgnore
    public static List<Map<String, String>> getDocumentTypes(){
        List<Map<String, String>> documentTypes = new ArrayList<>();
        documentTypes.add(Map.of("label", "Договор на подключение", "value", "ConnectionAgreementTemplate"));
        return documentTypes;
    }

    public String getTemporalId(){
        return UUID.randomUUID().toString();
    }

    @Override
    public void update(Form form) {}

    @Override
    public String getName() {
        return "Без типа";
    }

    public Map<String, String> toDtoMap(){
        return Map.of("documentTemplateId", String.valueOf(documentTemplateId), "type", type, "name", getName());
    }

    @Data
    public static class Form {
        @Nullable
        private Long documentTemplateId;
        private String type;
        @Nullable
        private String loginFieldId;
        @Nullable
        private String fullNameFieldId;
        @Nullable
        private String dateOfBirthFieldId;
        @Nullable
        private String regionOfBirthFieldId;
        @Nullable
        private String cityOfBirthFieldId;
        @Nullable
        private String passportDetailsFieldId;
        @Nullable
        private String addressFieldId;
        @Nullable
        private String phoneFieldId;
        @Nullable
        private String passwordFieldId;
        @Nullable
        private String tariffFieldId;

        public DocumentTemplate toEntity() {
            switch (getType()) {
                case "ConnectionAgreementTemplate" -> {
                    ConnectionAgreementTemplate connectionAgreementTemplate = new ConnectionAgreementTemplate();
                    connectionAgreementTemplate.setType(getType());
                    connectionAgreementTemplate.setLoginFieldId(getLoginFieldId());
                    connectionAgreementTemplate.setFullNameFieldId(getFullNameFieldId());
                    connectionAgreementTemplate.setDateOfBirthFieldId(getDateOfBirthFieldId());
                    connectionAgreementTemplate.setRegionOfBirthFieldId(getRegionOfBirthFieldId());
                    connectionAgreementTemplate.setCityOfBirthFieldId(getCityOfBirthFieldId());
                    connectionAgreementTemplate.setPassportDetailsFieldId(getPassportDetailsFieldId());
                    connectionAgreementTemplate.setAddressFieldId(getAddressFieldId());
                    connectionAgreementTemplate.setPhoneFieldId(getPhoneFieldId());
                    connectionAgreementTemplate.setPasswordFieldId(getPasswordFieldId());
                    connectionAgreementTemplate.setTariffFieldId(getTariffFieldId());
                    return connectionAgreementTemplate;
                }
                default -> throw new ResponseException("Неизвестный тип шаблона документа " + getType());
            }
        }
    }
}
