package com.microel.trackerbackend.storage.entities.storehouse;

import com.microel.trackerbackend.storage.entities.storehouse.util.RealizationType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "equipment_realizations")
public class EquipmentRealization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long equipmentRealizationId;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_equipment_stuff_id")
    private Stuff equipment;
    private Short count;
    private RealizationType realizationType;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}
