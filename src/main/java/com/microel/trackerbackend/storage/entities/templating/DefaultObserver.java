package com.microel.trackerbackend.storage.entities.templating;

import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private String name;
    @Enumerated(EnumType.STRING)
    private DefaultObserverTargetType targetType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultObserver that)) return false;
        return Objects.equals(getTargetId(), that.getTargetId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTargetId());
    }

    public static DefaultObserver from(Employee employee) {
        return DefaultObserver.builder()
                .targetId(employee.getLogin())
                .name(employee.getFullName())
                .targetType(DefaultObserverTargetType.EMPLOYEE)
                .build();
    }

    public static DefaultObserver from(Department department) {
        return DefaultObserver.builder()
                .targetId(department.getDepartmentId().toString())
                .name(department.getName())
                .targetType(DefaultObserverTargetType.DEPARTMENT)
                .build();
    }

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
