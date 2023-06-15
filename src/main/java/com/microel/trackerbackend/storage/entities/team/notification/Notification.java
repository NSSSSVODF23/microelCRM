package com.microel.trackerbackend.storage.entities.team.notification;

import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.WorkReport;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    @Column(length = 4096)
    private String message;
    private Timestamp created;
    private Boolean unread;
    private Timestamp whenRead;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Employee employee;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        Notification that = (Notification) o;
        return Objects.equals(getNotificationId(), that.getNotificationId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNotificationId());
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + notificationId +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", created=" + created +
                ", unread=" + unread +
                ", whenRead=" + whenRead +
                ", employee=" + employee +
                '}';
    }

    @AllArgsConstructor
    public static class Factory {

        private NotificationType type;

        private String message;
        public Notification getInstace(Employee recipient){
            return Notification.builder()
                    .type(type)
                    .message(message)
                    .created(Timestamp.from(Instant.now()))
                    .unread(true)
                    .whenRead(null)
                    .employee(recipient)
                    .build();
        }

    }
    public static Factory taskCreated(Task task){
        StringBuilder message =  new StringBuilder();

        message.append("Создана новая задача #")
                .append(task.getTaskId())
                .append(" пользователем @")
                .append(task.getCreator().getLogin());

        return new Factory(NotificationType.TASK_CREATED,  message.toString());
    }
    public static Factory taskHasBecomeActual(Task task) {
        StringBuilder message =  new StringBuilder();

        message.append("Задача #")
                .append(task.getTaskId())
                .append(" стала актуальной.");

        return new Factory(NotificationType.TASK_HAS_BECOME_ACTUAL, message.toString());
    }

    public static Factory taskExpired(Task task) {
        StringBuilder message =  new StringBuilder();

        message.append("Задача #")
                .append(task.getTaskId())
                .append(" срок истек.");

        return new Factory(NotificationType.TASK_EXPIRED, message.toString());
    }

    public static Factory reportReceived(WorkLog log, WorkReport report) {
        StringBuilder message =  new StringBuilder();

        message.append("Получен отчет о выполненых работах пользователем @")
                .append(report.getAuthor().getLogin())
                .append(" по задаче #").append(log.getTask().getTaskId());
        return new Factory(NotificationType.REPORT_RECEIVED, message.toString());
    }

    public static Factory worksCompleted(WorkLog log) {
        StringBuilder message =  new StringBuilder();

        message.append("Работы по задаче #").append(log.getTask().getTaskId()).append(" завершены.");
        return new Factory(NotificationType.WORKS_COMPLETED, message.toString());
    }

    public static Factory taskEdited(Task task, Employee editor){
        StringBuilder message =  new StringBuilder();

        message.append("Информация о задаче #")
                .append(task.getTaskId())
                .append(" изменена пользователем @")
                .append(editor.getLogin());

        return new Factory(NotificationType.TASK_EDITED, message.toString());
    }

    public static Factory taskClosed(Task task, Employee closer){
        StringBuilder message =  new StringBuilder();

        message.append("Задача #")
                .append(task.getTaskId())
                .append(" завершена пользователем @")
                .append(closer.getLogin());

        return new Factory(NotificationType.TASK_CLOSED, message.toString());
    }

    public static Factory taskDeleted(Task task, Employee remover){
        StringBuilder message =  new StringBuilder();

        message.append("Задача #")
                .append(task.getTaskId())
                .append(" удалена пользователем @")
                .append(remover.getLogin());

        return new Factory(NotificationType.TASK_DELETED, message.toString());
    }

    public static Factory taskProcessed(WorkLog log){
        StringBuilder message =  new StringBuilder();

        message.append("Задача #")
                .append(log.getTask().getTaskId())
                .append(" передана в работу специалистам ^(").append(log.getWorkLogId()).append("),")
                .append("пользователем @").append(log.getCreator().getLogin());

        return new Factory(NotificationType.TASK_PROCESSED, message.toString());
    }

    public static Factory taskReopened(Task task, Employee employee){
        StringBuilder message =  new StringBuilder();

        message.append("Задача #")
                .append(task.getTaskId())
                .append(" вновь открыта пользователем @")
                .append(employee.getLogin());

        return new Factory(NotificationType.TASK_REOPENED, message.toString());
    }

    public static Factory taskStageChanged(Task task, Employee employee){
        StringBuilder message =  new StringBuilder();

        message.append("Задача #")
                .append(task.getTaskId())
                .append(" переведена на стадию \"")
                .append(task.getCurrentStage().getLabel()).append("\"")
                .append(" пользователем @")
                .append(employee.getLogin());

        return new Factory(NotificationType.TASK_STAGE_CHANGED,  message.toString());
    }

    public static Factory youResponsible(Task task){
        StringBuilder message =  new StringBuilder();
        message.append("Вы назначены ответственным в задаче #")
                .append(task.getTaskId());
        return new Factory(NotificationType.YOU_RESPONSIBLE, message.toString());
    }

    public static Factory youObserver(Task task){
        StringBuilder message =  new StringBuilder();
        message.append("Вы назначены наблюдателем задачи #")
                .append(task.getTaskId());
        return new Factory(NotificationType.YOU_OBSERVER,  message.toString());
    }

    public static Factory newComment(Comment comment){

        // Отчищаем сообщение комментария от html тегов
        String commentMessage = comment.getMessage();
        // Сначала заменяем все теги </p> на символ переноса строки
        commentMessage = commentMessage.replaceAll("</p>", "\n");
        // Затем отчищаем всё остальное
        commentMessage = commentMessage.replaceAll("<.*?>", "");

        StringBuilder messageBuilder = new StringBuilder("@")
                .append(comment.getCreator().getLogin())
                .append(" оставил комментарий в задаче #").append(comment.getParent().getTaskId()).append("\n")
                .append(commentMessage);

        return new Factory(NotificationType.NEW_COMMENT, messageBuilder.toString());
    }

    public static Factory mentionedInTask(Task parent) {
        StringBuilder message =  new StringBuilder();
        message.append("Вас упомянули в задаче #");
        message.append(parent.getTaskId());
        return new Factory(NotificationType.MENTIONED_IN_TASK, message.toString());
    }

}
