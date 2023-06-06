package com.microel.trackerbackend.storage.entities.comments.events;

import com.microel.trackerbackend.storage.entities.comments.TaskJournalItem;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.Observer;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "task_events")
public class TaskEvent implements TaskJournalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskEventId;
    @Enumerated(EnumType.STRING)
    private TaskEventType type;
    @Column(columnDefinition = "text default ''")
    private String message;
    @ManyToOne
    @JoinColumn(name = "f_author_employee_id")
    private Employee creator;
    private Timestamp created;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Task task;

    public TaskEvent(Task task, TaskEventType type, Employee creator, String message) {
        this.task = task;
        this.type = type;
        this.creator = creator;
        this.message = message;
        this.created = Timestamp.from(Instant.now());
    }

    public static TaskEvent changeStage(Task task, TaskStage newStage, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.CHANGE_STAGE, employee, newStage.getLabel());
        return event;
    }

    public static TaskEvent createdWorkLog(Task task, WorkLog workLog, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.CREATE_WORK_LOG, employee, "^("+workLog.getWorkLogId()+")");
        return event;
    }

    public static TaskEvent forceCloseWorkLog(Task task, WorkLog workLog, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.FORCE_CLOSE_WORK_LOG, employee, "^("+workLog.getWorkLogId()+")");
        return event;
    }

    public static TaskEvent closeWorkLog(Task task, WorkLog workLog, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.CLOSE_WORK_LOG, employee, "^("+workLog.getWorkLogId()+")");
        return event;
    }

    public static TaskEvent changeObservers(Task task, List<Observer> observers, Employee employee) {
        TaskEvent event = new TaskEvent(
                task,
                TaskEventType.CHANGE_OBSERVERS,
                employee,
                observers.stream().map(Observer::getDesignation).collect(Collectors.joining(", "))
        );
        return event;
    }

    public static TaskEvent changeResponsible(Task task, Employee responsible, Employee employee) {
        TaskEvent event = new TaskEvent(
                task,
                TaskEventType.CHANGE_RESPONSIBILITY,
                employee,
                "@"+responsible.getLogin()
        );
        return event;
    }

    public static TaskEvent linkedToParentTask(Task task, Task parentTask, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.LINKED_TO_PARENT_TASK, employee, "#"+parentTask.getTaskId());
        return event;
    }

    public static TaskEvent unlinkFromParentTask(Task task, Task parentTask, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.UNLINKED_FROM_PARENT_TASK, employee, "#"+parentTask.getTaskId());
        return event;
    }

    public static TaskEvent unlinkChildTask(Task task, Set<Task> childs, Employee employee) {
        String linksToTasks = childs.stream().map(child->"#"+child.getTaskId()).collect(Collectors.joining(", "));
        TaskEvent event = new TaskEvent(task, TaskEventType.UNLINK_CHILD_TASK, employee, linksToTasks);
        return event;
    }

    public static TaskEvent linkedToChildTask(Task task, Set<Task> childs, Employee employee) {
        String linksToTasks = childs.stream().map(child->"#"+child.getTaskId()).collect(Collectors.joining(", "));
        TaskEvent event = new TaskEvent(task, TaskEventType.LINKED_TO_CHILD_TASKS, employee, linksToTasks);
        return event;
    }

    public static TaskEvent changeTags(Task task, Set<TaskTag> tags, Employee employee) {
        TaskEvent event = null;
        if(tags.isEmpty()){
            event = new TaskEvent(task, TaskEventType.CLEAN_TAGS, employee, "");
        }else{
            event = new TaskEvent(task, TaskEventType.CHANGE_TAGS, employee, tags.stream().map(TaskTag::getName).collect(Collectors.joining(", ")));
        }
        return event;
    }

    public static TaskEvent unbindResponsible(Task task, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.UNBIND_RESPONSIBLE, employee, "");
        return event;
    }

    public static TaskEvent changeActualFrom(Task task, Instant datetime, Employee employee) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        TaskEvent event = new TaskEvent(task, TaskEventType.CHANGE_ACTUAL_FROM, employee, formatter.format(ZonedDateTime.ofInstant(datetime, ZoneId.systemDefault())));
        return event;
    }

    public static TaskEvent changeActualTo(Task task, Instant datetime, Employee employee) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        TaskEvent event = new TaskEvent(task, TaskEventType.CHANGE_ACTUAL_TO, employee, formatter.format(ZonedDateTime.ofInstant(datetime, ZoneId.systemDefault())));
        return event;
    }

    public static TaskEvent clearActualFrom(Task task, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.CLEAR_ACTUAL_FROM_TASK, employee, "");
        return event;
    }

    public static TaskEvent clearActualTo(Task task, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.CLEAR_ACTUAL_TO_TASK, employee, "");
        return event;
    }

    public static TaskEvent close(Task task, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.CLOSE_TASK, employee, "");
        return event;
    }

    public static TaskEvent reopen(Task task, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.REOPEN_TASK, employee, "");
        return event;
    }

    public static TaskEvent editFields(Task task, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.EDIT_FIELDS, employee, "");
        return event;
    }

    public static TaskEvent reportCreated(Task task, String report, Employee employee) {
        TaskEvent event = new TaskEvent(task, TaskEventType.REPORT_CREATED, employee, report);
        return event;
    }
}
