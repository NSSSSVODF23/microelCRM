package com.microel.trackerbackend.services.api;

import com.microel.trackerbackend.misc.SimpleMessage;
import com.microel.trackerbackend.parsers.OldTracker;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.events.TaskEvent;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Controller
public class StompController {
    private final SimpMessagingTemplate stompBroker;

    public StompController(SimpMessagingTemplate stompBroker) {
        this.stompBroker = stompBroker;
    }

    public void createTask(Task task) {
        stompBroker.convertAndSend("/task/create", task);
    }

    public void updateTask(Task task){
        stompBroker.convertAndSend("/task/" + task.getTaskId() + "/update", task);
        stompBroker.convertAndSend("/task/update", task);
    }

    public void deleteTask(Task task){
        stompBroker.convertAndSend("/task/" + task.getTaskId() + "/delete", task);
        stompBroker.convertAndSend("/task/delete", task);
    }

    public void createTaskEvent(Long taskId, TaskEvent taskEvent){
        stompBroker.convertAndSend("/task/" + taskId + "/event/create", taskEvent);
    }

    public void createComment(Comment comment){
        stompBroker.convertAndSend("/task/" + comment.getParent().getTaskId() + "/comment/create", comment.toDto());
    }

    public void updateComment(Comment comment){
        stompBroker.convertAndSend("/task/" + comment.getParent().getTaskId() + "/comment/update", comment.toDto());
    }

    public void deleteComment(Comment comment){
        stompBroker.convertAndSend("/task/" + comment.getParent().getTaskId() + "/comment/delete", comment.toDto());
    }

    public void updateEmployee(Employee employee) {
        stompBroker.convertAndSend("/employee/update", employee);
        stompBroker.convertAndSend("/employee/"+employee.getLogin()+"/update", employee);
    }

    public void createTaskTag(TaskTag taskTag) {
        stompBroker.convertAndSend("/task-tag/create", taskTag);
    }

    public void updateTaskTag(TaskTag taskTag) {
        stompBroker.convertAndSend("/task-tag/update", taskTag);
    }

    public void deleteTaskTag(TaskTag taskTag) {
        stompBroker.convertAndSend("/task-tag/delete", taskTag);
    }

    @Scheduled(fixedDelay = 5000)
    private void sendNotification() {
        stompBroker.convertAndSendToUser("testUser123","/noty/test", "{\"test\":123}");
    }

    public void sendNotification(Notification notification) {
        stompBroker.convertAndSendToUser(notification.getEmployee().getLogin(), "/notification/create", notification);
    }

    public void updateNotification(Notification notification) {
        stompBroker.convertAndSendToUser(notification.getEmployee().getLogin(), "/notification/update", notification);
    }

    public void createMessage(ChatMessage message) {
        stompBroker.convertAndSend("/chat/"+message.getParentChat().getChatId()+"/message/create", message);
    }

    public void updateTrackerParser(OldTracker.DTO tracker) {
        stompBroker.convertAndSend("/parser/tracker/update", tracker);
    }

    public void sendParserMessage(SimpleMessage simpleMessage) {
        stompBroker.convertAndSend("/parser/message", simpleMessage);
    }
}
