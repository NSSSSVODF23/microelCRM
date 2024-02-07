package com.microel.trackerbackend.storage.entities;

import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "employee_interventions")
public class EmployeeIntervention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeInterventionId;
    @ManyToOne
    @JoinColumn(name = "f_employee_login")
    private Employee employee;
    private Timestamp timestamp;
    @Nullable
    @Column(columnDefinition = "text default ''")
    private String description;

    public static EmployeeIntervention from(Employee employee, @Nullable String description) {
        EmployeeIntervention employeeIntervention = new EmployeeIntervention();
        employeeIntervention.setEmployee(employee);
        employeeIntervention.setDescription(description);
        employeeIntervention.setTimestamp(Timestamp.from(Instant.now()));
        return employeeIntervention;
    }

    public static EmployeeIntervention from(Employee employee) {
        return from(employee, null);
    }
}
