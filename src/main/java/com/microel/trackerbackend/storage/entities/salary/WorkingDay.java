package com.microel.trackerbackend.storage.entities.salary;

import com.microel.trackerbackend.misc.SalaryRow;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "working_days")
public class WorkingDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workingDayId;
    private Date date;
    @ManyToOne
    @JoinColumn(name = "f_employee_login")
    private Employee employee;
    @ManyToMany
    @BatchSize(size = 25)
    private List<WorkCalculation> calculations;

    public SalaryRow.SalaryPoint toSalaryPoint(){
        return SalaryRow.SalaryPoint.builder()
                .date(date)
                .value(calculations.stream().map(WorkCalculation::getSum).reduce(0,Integer::sum))
                .build();
    }
}
