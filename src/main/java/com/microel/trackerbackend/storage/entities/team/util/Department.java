package com.microel.trackerbackend.storage.entities.team.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.Observer;
import com.microel.trackerbackend.storage.entities.templating.DefaultObserver;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "departments")
public class Department implements Observer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;
    @Column(length = 48)
    private String name;
    @Column(length = 255)
    private String description;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    private Timestamp created;
    @OneToMany(mappedBy = "department")
    @JsonIgnore
    @BatchSize(size = 25)
    private Set<Employee> employees;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        Department that = (Department) o;
        return Objects.equals(getDepartmentId(), that.getDepartmentId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDepartmentId());
    }

    @Override
    @JsonIgnore
    public String getIdentifier() {
        return getDepartmentId().toString();
    }

    @Override
    @JsonIgnore
    public String getDesignation() {
        return "$"+getDepartmentId();
    }

    public static Department from(DefaultObserver defaultObserver){
        return Department.builder().departmentId(Long.valueOf(defaultObserver.getTargetId())).build();
    }
}
