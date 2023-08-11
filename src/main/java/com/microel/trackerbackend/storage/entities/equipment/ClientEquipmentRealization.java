package com.microel.trackerbackend.storage.entities.equipment;

import com.microel.trackerbackend.misc.AbstractForm;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "client_equipment_realizations")
public class ClientEquipmentRealization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientEquipmentRealizationId;
    @ManyToOne
    private ClientEquipment equipment;
    private Integer count;

    @Getter
    @Setter
    public static class Form implements AbstractForm{
        private ClientEquipment equipment;
        private Integer count;

        @Override
        public boolean isValid() {
            return equipment != null && count != null && count > 0;
        }
    }
}
