package com.microel.trackerbackend.storage.entities.templating;

import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "default_observers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultObserver {
    @Id
    private String targetId;
    @Enumerated(EnumType.STRING)
    private DefaultObserverTargetType targetType;

    public static List<Department> getSetOfDepartments(List<DefaultObserver> defaultObservers) {
        return defaultObservers.stream()
                .filter(t -> t.getTargetType() == DefaultObserverTargetType.DEPARTMENT)
                .map(Department::from).collect(Collectors.toList());
    }

    public static List<Employee> getSetOfEmployees(List<DefaultObserver> defaultObservers) {
        return defaultObservers.stream()
                .filter(t -> t.getTargetType() == DefaultObserverTargetType.EMPLOYEE)
                .map(Employee::from).collect(Collectors.toList());
    }
}
