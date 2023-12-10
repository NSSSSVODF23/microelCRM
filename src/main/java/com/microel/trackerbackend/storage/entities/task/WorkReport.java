package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "work_reports")
public class WorkReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workReportId;
    @Column(columnDefinition = "text default ''")
    private String description;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference
    private WorkLog workLog;
    @OneToOne()
    private Employee author;
    private Timestamp created;
    @Column(columnDefinition = "boolean default false")
    private Boolean awaitingWriting;
}
