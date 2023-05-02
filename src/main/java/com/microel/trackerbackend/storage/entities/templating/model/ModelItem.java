package com.microel.trackerbackend.storage.entities.templating.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.address.Address;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "models_items")
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
        return "{"+name+":"+getValue()+"}";
    }

    public ModelItem cleanToCreate() {
        modelItemId = null;
        task = null;
        return this;
    }

    @JsonIgnore
    public Object getValue() {
        switch (wireframeFieldType) {
            case ADDRESS:
                return addressData;
            case BOOLEAN:
                return booleanData;
            case FLOAT:
                return floatData;
            case INTEGER:
                return integerData;
            case LARGE_TEXT:
            case LOGIN:
            case SMALL_TEXT:
            case CONNECTION_SERVICES:
            case EQUIPMENTS:
            case IP:
            case REQUEST_INITIATOR:
            case AD_SOURCE:
                return stringData;
            case PHONE_ARRAY:
                return phoneData;
            default:
                return null;
        }
    }

    public void setValue(Object value) {
        switch (wireframeFieldType) {
            case ADDRESS:
                addressData = (Address) value;
                break;
            case BOOLEAN:
                booleanData = (Boolean) value;
                break;
            case FLOAT:
                floatData = (Float) value;
                break;
            case INTEGER:
                integerData = (Integer) value;
                break;
            case LARGE_TEXT:
            case LOGIN:
            case SMALL_TEXT:
            case CONNECTION_SERVICES:
            case EQUIPMENTS:
            case IP:
            case REQUEST_INITIATOR:
            case AD_SOURCE:
                stringData = (String) value;
                break;
            case PHONE_ARRAY:
                phoneData = (Map<String, String>) value;
                break;
            default:
                break;
        }
    }
}
