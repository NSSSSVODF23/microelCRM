package com.microel.trackerbackend.services.api;

import com.microel.trackerbackend.configurations.StompConfig;
import com.microel.trackerbackend.misc.SimpleMessage;
import com.microel.trackerbackend.parsers.oldtracker.OldTracker;
import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.comment.CommentDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.SuperMessage;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.entities.team.util.Position;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class StompController {
    private final SimpMessagingTemplate stompBroker;

    public StompController(SimpMessagingTemplate stompBroker) {
        this.stompBroker = stompBroker;
    }

    private void sendAll(Object payload, String... patch) {
        String destination = String.join("/", patch);
        stompBroker.convertAndSend(StompConfig.SIMPLE_BROKER_PREFIX + "/" + destination, payload);
    }

    private void sendToUser(String login, Object payload, String... patch) {
        String destination = String.join("/", patch);
        stompBroker.convertAndSendToUser(login, destination, payload);
    }

    public void createTask(Task task) {
        sendAll(task, "task", "create");
    }

    public void updateTask(Task task) {
        sendAll(task, "task", task.getTaskId().toString(), "update");
        sendAll(task, "task", "update");
    }

    public void deleteTask(Task task) {
        sendAll(task, "task", task.getTaskId().toString(), "delete");
        sendAll(task, "task", "delete");
    }

    public void createTaskEvent(Long taskId, TaskEvent taskEvent) {
        sendAll(taskEvent, "task", taskId.toString(), "event", "create");
    }

    public void createComment(CommentDto comment, String parentTaskId) {
        sendAll(comment, "task", parentTaskId, "comment", "create");
    }

    public void updateComment(CommentDto comment, String parentTaskId) {
        sendAll(comment, "task", parentTaskId, "comment", "update");
    }

    public void deleteComment(CommentDto comment, String parentTaskId) {
        sendAll(comment, "task", parentTaskId, "comment", "delete");
    }

    public void createEmployee(Employee employee) {
        sendAll(employee, "employee", "create");
    }

    public void updateEmployee(Employee employee) {
        sendAll(employee, "employee", employee.getLogin(), "update");
        sendAll(employee, "employee", "update");
    }

    public void deleteEmployee(Employee employee) {
        sendAll(employee, "employee", "delete");
    }

    public void createDepartment(Department department) {
        sendAll(department, "department", "create");
    }

    public void updateDepartment(Department department) {
        sendAll(department, "department", department.getDepartmentId().toString(), "update");
        sendAll(department, "department", "update");
    }

    public void deleteDepartment(Department department) {
        sendAll(department, "department", "delete");
    }

    public void createPosition(Position position) {
        sendAll(position, "position", "create");
    }

    public void updatePosition(Position position) {
        sendAll(position, "position", position.getPositionId().toString(), "update");
        sendAll(position, "position", "update");
    }

    public void deletePosition(Position position) {
        sendAll(position, "position", "delete");
    }

    public void createTaskTag(TaskTag taskTag) {
        sendAll(taskTag, "task-tag", "create");
    }

    public void updateTaskTag(TaskTag taskTag) {
        sendAll(taskTag, "task-tag", "update");
    }

    public void deleteTaskTag(TaskTag taskTag) {
        sendAll(taskTag, "task-tag", "delete");
    }

    public void sendNotification(Notification notification) {
        sendToUser( notification.getEmployee().getLogin(), notification, "notification", "create");
    }

    public void updateNotification(Notification notification) {
        sendToUser( notification.getEmployee().getLogin(), notification, "notification", "update");
    }

    public void updateChat(Chat chat) {
        sendAll(chat, "chat", "update");
    }

    public void closeChat(Chat chat) {
        sendAll(chat, "chat", "close");
    }

    public void createMessage(SuperMessage message) {
        sendAll(message, "chat", message.getParentChatId().toString(), "message", "create");
    }

    public void updateMessage(SuperMessage message) {
        sendAll(message, "chat", message.getParentChatId().toString(), "message", "update");
    }

    public void deleteMessage(SuperMessage deletedMessage) {
        sendAll(deletedMessage, "chat", deletedMessage.getParentChatId().toString(), "message", "delete");
    }

    public void updateCountUnreadMessage(String login, Long chatId, Long count) {
        sendToUser(login, new Chat.UnreadCounter(chatId, count), "chat", "message", "unread");
    }

    public void updateTrackerParser(OldTracker.DTO tracker) {
        sendAll(tracker, "parser", "tracker", "update");
    }

    public void sendParserMessage(SimpleMessage simpleMessage) {
        sendAll(simpleMessage, "parser", "message");
    }

    public void createChat(ChatDto chat) {
        for(EmployeeDto employee : chat.getMembers()) {
            sendToUser(employee.getLogin(), chat, "chat", "create");
        }
    }
}
