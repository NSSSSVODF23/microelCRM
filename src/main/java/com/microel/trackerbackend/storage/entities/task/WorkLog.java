package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.task.utils.AcceptingEntry;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "work_logs")
@TypeDef(name = "json", typeClass = JsonType.class)
public class WorkLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workLogId;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
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
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Set<AcceptingEntry> acceptedEmployees;
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

    public Set<AcceptingEntry> getAcceptedEmployees() {
        if(acceptedEmployees == null) return acceptedEmployees = new HashSet<>();
        return acceptedEmployees;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWorkLogId());
    }
}
