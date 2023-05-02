package com.microel.trackerbackend.storage.entities.task.utils;

import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "task_initiators")
public class TaskInitiator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskInitiatorId;
    private TaskSource taskSource;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "employee_employee_id")
    private Employee employee;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}
