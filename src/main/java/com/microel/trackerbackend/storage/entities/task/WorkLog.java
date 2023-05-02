package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "work_logs")
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workLogId;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Chat chat;
    @OneToMany(mappedBy = "workLog")
    @JsonManagedReference
    @BatchSize(size = 25)
    private Set<WorkReport> workReports;
    private Timestamp created;
    private Timestamp closed;
    @Column(columnDefinition = "boolean default false")
    private Boolean isForceClosed = false;
    @ManyToMany()
    @BatchSize(size = 25)
    private Set<Employee> employees;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_task_id")
    private Task task;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_creator_id")
    private Employee creator;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkLog)) return false;
        WorkLog workLog = (WorkLog) o;
        return Objects.equals(getWorkLogId(), workLog.getWorkLogId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWorkLogId());
    }
}
