package com.microel.trackerbackend.storage.entities.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.task.utils.AcceptingEntry;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.lang.Nullable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    @OneToMany(mappedBy = "workLog", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JsonManagedReference
    @BatchSize(size = 25)
    private Set<WorkReport> workReports;
    private Timestamp created;
    private Timestamp closed;
    @Column(columnDefinition = "boolean default false")
    private Boolean isForceClosed = false;
    @Column(columnDefinition = "text default ''")
    private String forceClosedReason;
    @Column(columnDefinition = "text default ''")
    private String targetDescription;
    @ManyToMany()
    @BatchSize(size = 25)
    private Set<Employee> employees;
    @Nullable
    private String gangLeader;
    @Column(columnDefinition = "boolean default false")
    private Boolean deferredReport;
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Set<AcceptingEntry> acceptedEmployees;
    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_task_id")
    private Task task;
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_creator_id")
    private Employee creator;
    @Column(columnDefinition = "boolean default false")
    private Boolean calculated;
    @OneToMany
    @JsonIgnore
    @BatchSize(size = 25)
    private List<WorkCalculation> workCalculations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkLog)) return false;
        WorkLog workLog = (WorkLog) o;
        return Objects.equals(getWorkLogId(), workLog.getWorkLogId());
    }

    public Set<AcceptingEntry> getAcceptedEmployees() {
        if (acceptedEmployees == null) return acceptedEmployees = new HashSet<>();
        return acceptedEmployees;
    }

    /**
     * Статус текущего журнала задач
     *
     * @return Текст описывающий текущее состояние журнала задачи
     */
    public Status getStatus() {
        if (isForceClosed) {
            return Status.FORCE_CLOSE;
        }else if(closed != null) {
            return Status.CLOSE;
        } else {
            return Status.ACTIVE;
        }
    }

    /**
     * Список сотрудников которые приняли задачу
     *
     * @return Список сотрудников
     */
    public Set<Employee> getWhoAccepted() {
        return employees.stream().filter(e -> acceptedEmployees.stream().anyMatch(a -> a.getLogin().equals(e.getLogin()))).collect(Collectors.toSet());
    }

    /**
     * Список сотрудников которые завершили задачу
     *
     * @return Список сотрудников
     */
    public Set<Employee> getWhoClosed() {
        if(workReports == null || workReports.isEmpty()) return new HashSet<>();
        return workReports.stream().map(WorkReport::getAuthor).collect(Collectors.toSet());
    }

    /** Список сотрудников кто принял задачу и ещё не закрыл её (активные)
     *
     * @return Список сотрудников
     */
    public Set<Employee> getWhoActive() {
        return getWhoAccepted().stream().filter(employee -> !getWhoClosed().contains(employee)).collect(Collectors.toSet());
    }

    /**
     * Общий отчет о выполненных работах или о причине завершения задачи
     *
     * @return Общий отчет
     */
    public String getReport() {
        if (getStatus() == Status.FORCE_CLOSE || workReports == null || workReports.isEmpty()) {
            if (forceClosedReason != null) {
                return forceClosedReason;
            }
            return "";
        }
        return workReports.stream()
                .map(workReport -> workReport.getAuthor().getFullName() + ": " + workReport.getDescription())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Затраченное время на выполнение задачи
     *
     * @return Затраченное время в миллисекундах
     */
    public Long getLeadTime() {
        if (getStatus() == Status.ACTIVE) return 0L;
        return closed.getTime() - created.getTime();
    }

    /**
     * Добавляет отчет в список отчетов
     *
     * @return Текущий журнал
     */
    public WorkLog addWorkReport(WorkReport workReport) {
        if (workReports == null) workReports = new HashSet<>();
        workReport.setWorkLog(this);
        workReports.add(workReport);
        return this;
    }

    public boolean getIsReportsUncompleted(){
        if(getIsForceClosed()) return false;
        if(workReports == null || workReports.isEmpty()) return true;
        if(workReports.size() != getWorkReports().size()) return true;
        return workReports.stream().anyMatch(WorkReport::getAwaitingWriting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWorkLogId());
    }

    public enum Status {
        ACTIVE("ACTIVE"), CLOSE("CLOSE"), FORCE_CLOSE("FORCE_CLOSE");

        private final String status;

        Status(String status) {
            this.status = status;
        }
    }

    @Getter
    @Setter
    public static class AssignBody{
        private Set<Employee> installers;
        @Nullable
        private String gangLeader;
        private Boolean deferredReport;
        private String description;
    }

    @Getter
    @Setter
    public static class WritingReportForm{
        private String reportDescription;
        private Long workLogId;

        public void throwIfIncomplete(){
            if(reportDescription == null || reportDescription.isBlank()) throw new ResponseException("Отчет не заполнен");
            if(workLogId == null) throw new ResponseException("Не указан id журнала задачи");
        }
    }
}
