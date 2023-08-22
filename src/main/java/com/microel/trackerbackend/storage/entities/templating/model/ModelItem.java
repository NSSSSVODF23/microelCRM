package com.microel.trackerbackend.storage.entities.templating.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microel.trackerbackend.controllers.telegram.handle.Decorator;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.equipment.ClientEquipmentRealization;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.templating.ConnectionType;
import com.microel.trackerbackend.storage.entities.templating.DataConnectionService;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "models_items")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long modelItemId;
    @ManyToOne(cascade = {CascadeType.PERSIST}, targetEntity = Task.class)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JsonBackReference
    private Task task;
    @Column(length = 40)
    private String id;
    @Column(length = 48)
    private String name;
    @Enumerated(EnumType.STRING)
    private WireframeFieldType wireframeFieldType;
    private String variation;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "f_address_id")
    private Address addressData;
    private Boolean booleanData;
    private Float floatData;
    private Integer integerData;
    @Column(columnDefinition = "text")
    private String stringData;
    private Timestamp timestampData;
    @ElementCollection
    @CollectionTable(name = "phone_numbers", joinColumns = @JoinColumn(name = "f_model_id"))
    @MapKeyColumn(name = "phone_id", length = 50)
    @Column(name = "phone", length = 20)
    private Map<String, String> phoneData;
    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST})
    @BatchSize(size = 25)
    private List<DataConnectionService> connectionServicesData;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private List<ClientEquipmentRealization> equipmentRealizationsData;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelItem)) return false;
        ModelItem modelItem = (ModelItem) o;
        return Objects.equals(getModelItemId(), modelItem.getModelItemId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getModelItemId());
    }

    @Override
    public String toString() {
        return "{" + name + ":" + getValue() + "}";
    }

    public ModelItem cleanToCreate() {
        modelItemId = null;
        task = null;
        return this;
    }

    @JsonIgnore
    public Object getValue() {
        return switch (wireframeFieldType) {
            case ADDRESS -> addressData;
            case BOOLEAN -> booleanData;
            case FLOAT -> floatData;
            case INTEGER -> integerData;
            case LARGE_TEXT, LOGIN, SMALL_TEXT, CONNECTION_TYPE, IP, REQUEST_INITIATOR, AD_SOURCE -> stringData;
            case CONNECTION_SERVICES -> connectionServicesData;
            case PHONE_ARRAY -> phoneData;
            case EQUIPMENTS -> equipmentRealizationsData;
        };
    }

    public void setValue(Object value) {
        switch (wireframeFieldType) {
            case ADDRESS -> addressData = (Address) value;
            case BOOLEAN -> booleanData = (Boolean) value;
            case FLOAT -> floatData = (Float) value;
            case INTEGER -> integerData = (Integer) value;
            case LARGE_TEXT, LOGIN, SMALL_TEXT, CONNECTION_TYPE, IP, REQUEST_INITIATOR, AD_SOURCE -> stringData = (String) value;
            case CONNECTION_SERVICES -> connectionServicesData = (List<DataConnectionService>) value;
            case PHONE_ARRAY -> phoneData = (Map<String, String>) value;
            case EQUIPMENTS -> equipmentRealizationsData = (List<ClientEquipmentRealization>) value;
            default -> {
            }
        }
    }

    public String getTextRepresentation() {
        switch (wireframeFieldType) {
            case LARGE_TEXT:
            case SMALL_TEXT:
            case IP:
            case REQUEST_INITIATOR:
            case AD_SOURCE:
            case LOGIN:
                return stringData;
            case CONNECTION_TYPE:
                return stringData == null ? "" : ConnectionType.valueOf(stringData).getLabel();
            case INTEGER:
                return String.valueOf(integerData);
            case FLOAT:
                return String.valueOf(floatData);
            case BOOLEAN:
                return booleanData ? "Да" : "Нет";
            case ADDRESS:
                if(addressData == null) {
                    return "Ошибка чтения адреса";
                }
                StringBuilder addressResult = new StringBuilder();
                if (addressData.getCity() != null) {
                    addressResult.append(addressData.getCity().getName());
                }
                if (addressData.getStreet() != null) {
                    addressResult.append(" ").append(addressData.getStreet().getName());
                }
                if (addressData.getHouseNum() != null) {
                    addressResult.append(" ").append(addressData.getHouseNum());
                }
                if (addressData.getFraction() != null) {
                    addressResult.append("/").append(addressData.getFraction());
                }
                if (addressData.getLetter() != null) {
                    addressResult.append(addressData.getLetter());
                }
                if (addressData.getBuild() != null) {
                    addressResult.append(" стр.").append(addressData.getBuild());
                }
                if (addressData.getEntrance() != null) {
                    addressResult.append(" под.").append(addressData.getEntrance());
                }
                if (addressData.getFloor() != null) {
                    addressResult.append(" эт.").append(addressData.getFloor());
                }
                if (addressData.getApartmentNum() != null) {
                    addressResult.append(" кв.").append(addressData.getApartmentNum());
                }
                if (addressData.getApartmentMod() != null) {
                    addressResult.append(" ").append(addressData.getApartmentMod());
                }
                return addressResult.toString();
            case PHONE_ARRAY:
                return String.join(", ", phoneData.values());
            case CONNECTION_SERVICES:
                return connectionServicesData.stream().map(val->val.getConnectionService().getLabel()).collect(Collectors.joining(", "));
            case EQUIPMENTS:
                return equipmentRealizationsData.stream().map(val->val.getEquipment().getName()+" "+val.getCount()+" шт.").collect(Collectors.joining(", "));
            default:
                return null;
        }
    }

    @JsonIgnore
    public String getTextRepresentationForTlg() {
        switch (wireframeFieldType) {
            case LARGE_TEXT:
            case SMALL_TEXT:
            case IP:
            case REQUEST_INITIATOR:
            case AD_SOURCE:
            case LOGIN:
                return stringData;
            case CONNECTION_TYPE:
                return stringData == null ? "" : ConnectionType.valueOf(stringData).getLabel();
            case INTEGER:
                return String.valueOf(integerData);
            case FLOAT:
                return String.valueOf(floatData);
            case BOOLEAN:
                return booleanData ? "Да" : "Нет";
            case ADDRESS:
                if(addressData == null) {
                    return "Ошибка чтения адреса";
                }
                StringBuilder addressResult = new StringBuilder();
                if (addressData.getCity() != null) {
                    addressResult.append(addressData.getCity().getName());
                }
                if (addressData.getStreet() != null) {
                    addressResult.append(" ").append(addressData.getStreet().getName());
                }
                if (addressData.getHouseNum() != null) {
                    addressResult.append(" ").append(addressData.getHouseNum());
                }
                if (addressData.getFraction() != null) {
                    addressResult.append("/").append(addressData.getFraction());
                }
                if (addressData.getLetter() != null) {
                    addressResult.append(addressData.getLetter());
                }
                if (addressData.getBuild() != null) {
                    addressResult.append(" стр.").append(addressData.getBuild());
                }
                if (addressData.getEntrance() != null) {
                    addressResult.append(" под.").append(addressData.getEntrance());
                }
                if (addressData.getFloor() != null) {
                    addressResult.append(" эт.").append(addressData.getFloor());
                }
                if (addressData.getApartmentNum() != null) {
                    addressResult.append(" кв.").append(addressData.getApartmentNum());
                }
                if (addressData.getApartmentMod() != null) {
                    addressResult.append(" ").append(addressData.getApartmentMod());
                }
                return addressResult.toString();
            case PHONE_ARRAY:
                return phoneData.values().stream().map(phone-> "+7"+phone.substring(1).replaceAll(" ","")).map(Decorator::phone).collect(Collectors.joining("\n"));
            case CONNECTION_SERVICES:
                return connectionServicesData.stream().map(val->val.getConnectionService().getLabel()).collect(Collectors.joining(", "));
            case EQUIPMENTS:
                return equipmentRealizationsData.stream().map(val->val.getEquipment().getName() + " " + val.getCount() + " шт.").collect(Collectors.joining(", "));
            default:
                return null;
        }
    }
}
