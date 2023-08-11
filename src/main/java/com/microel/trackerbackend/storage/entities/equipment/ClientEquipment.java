package com.microel.trackerbackend.storage.entities.equipment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microel.trackerbackend.misc.AbstractForm;
import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "client_equipments")
public class ClientEquipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientEquipmentId;
    @Column(columnDefinition = "text default ''")
    private String name;
    @Nullable
    @Column(columnDefinition = "text default ''")
    private String description;
    private Float price;
    private Timestamp created;
    @ManyToOne
    private Employee creator;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private List<EmployeeIntervention> editedBy;
    private Boolean deleted;

    public void addEditedBy(Employee employee, String description) {
        this.editedBy.add(EmployeeIntervention.builder()
                .employee(employee)
                .description(description)
                .timestamp(Timestamp.from(Instant.now()))
                .build());
    }

    @Nullable
    public EmployeeIntervention getLastEdit(){
        if(editedBy == null) return null;
        return editedBy.stream().max(Comparator.comparing(EmployeeIntervention::getTimestamp)).orElse(null);
    }

    @Getter
    @Setter
    public static class Form implements AbstractForm {
        private String name;
        @Nullable
        private String description;
        private Float price;

        @Override
        public boolean isValid() {
            return name != null && !name.isBlank() && price != null && price > 0.0f;
        }
    }
}
