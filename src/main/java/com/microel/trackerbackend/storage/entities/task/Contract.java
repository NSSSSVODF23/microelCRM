package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contractId;
    @ManyToOne
    @JoinColumn(name = "f_type_of_contract_id")
    private TypesOfContracts typeOfContract;
    private Long count;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "f_work_log_id")
    private WorkLog workLog;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_contract_received_id")
    @Nullable
    private EmployeeIntervention received;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_contract_archived_id")
    @Nullable
    private EmployeeIntervention archived;

    @JsonIgnore
    public boolean isUnreceived(){
        return received == null;
    }

    @JsonIgnore
    public String toTelegramString(){
        return typeOfContract.getName() + " кол-во: " + count;
    }
}
