package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "types_of_contracts")
public class TypesOfContracts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long typeOfContractId;
    private String name;
    @Nullable
    private String description;
    @ManyToMany
    @JoinTable(name="contracts_receivers",
            joinColumns=  @JoinColumn(name="f_type_of_contract_id", referencedColumnName="typeOfContractId"),
            inverseJoinColumns= @JoinColumn(name="employee_login", referencedColumnName="login"))
    @BatchSize(size = 25)
    private List<Employee> receivers;
    @ManyToMany
    @JoinTable(name="contracts_archivers",
            joinColumns=  @JoinColumn(name="f_type_of_contract_id", referencedColumnName="typeOfContractId"),
            inverseJoinColumns= @JoinColumn(name="employee_login", referencedColumnName="login"))
    @BatchSize(size = 25)
    private List<Employee> archivers;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_created_by_id")
    private EmployeeIntervention createdBy;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @OrderBy("timestamp DESC")
    @BatchSize(size = 25)
    @JoinTable(name="types_contracts_update_by",
            joinColumns=  @JoinColumn(name="f_type_of_contract_id", referencedColumnName="typeOfContractId"),
            inverseJoinColumns= @JoinColumn(name="f_employee_intervention_id", referencedColumnName="employeeInterventionId"))
    private List<EmployeeIntervention> updatedBy;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @OrderBy("timestamp DESC")
    @BatchSize(size = 25)
    @JoinTable(name="types_contracts_delete_by",
            joinColumns=  @JoinColumn(name="f_type_of_contract_id", referencedColumnName="typeOfContractId"),
            inverseJoinColumns= @JoinColumn(name="f_employee_intervention_id", referencedColumnName="employeeInterventionId"))
    private List<EmployeeIntervention> deletedBy;
    @Column(columnDefinition = "boolean default false")
    private Boolean isDeleted;

    @Nullable
    public EmployeeIntervention getUpdatedBy(){
        if(updatedBy == null || updatedBy.isEmpty())
            return null;
    	return updatedBy.get(0);
    }

    @Nullable
    public EmployeeIntervention getDeletedBy(){
        if(deletedBy == null || deletedBy.isEmpty())
            return null;
        return deletedBy.get(0);
    }

    @JsonIgnore
    public List<EmployeeIntervention> getUpdatedByList(){
        return updatedBy;
    }

    @JsonIgnore
    public List<EmployeeIntervention> getDeletedByList(){
        return deletedBy;
    }

    public Suggestion toSuggestion(){
        return new Suggestion(name, typeOfContractId);
    }

    @Data
    @NoArgsConstructor
    public static class Form {
        @NotBlank
        private String name;
        private String description;
        @NonNull
        private List<String> receivers;
        @NonNull
        private List<String> archivers;
    }

    @Data
    public static class Suggestion {
        @NonNull
        private String label;
        @NonNull
        private Long value;
    }
}
