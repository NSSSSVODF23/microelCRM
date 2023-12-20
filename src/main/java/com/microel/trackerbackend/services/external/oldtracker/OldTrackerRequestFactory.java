package com.microel.trackerbackend.services.external.oldtracker;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.oldtracker.requests.*;
import com.microel.trackerbackend.services.external.oldtracker.task.TaskFieldOT;
import lombok.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class OldTrackerRequestFactory {

    private String username;
    private String password;

    private Map<String,String> cookies;

    public OldTrackerRequestFactory(String username, String password){
        setUsername(username);
        setPassword(password);
        try {
            Connection.Response response = Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=list_objis").headers(getAuthHeader()).method(Connection.Method.GET).ignoreHttpErrors(true).execute();
            setCookies(response.cookies());
        } catch (IOException e) {
            throw new ResponseException(e.getMessage());
        }
    }

    private Map<String,String> getAuthHeader(){
        String login = username + ":" + password;
        String base64login = Base64.getEncoder().encodeToString(login.getBytes());
        return Map.of("Authorization", "Basic "+base64login);
    }

    public NewTaskIdRequest getNewTaskId(Integer taskClassId){
        return new NewTaskIdRequest(getAuthHeader(), cookies, taskClassId);
    }

    public CreateTaskRequest createTask(Long taskId, Integer stageId, List<FieldData> fields){
        return new CreateTaskRequest(getAuthHeader(), cookies, taskId, stageId, fields);
    }

    public CreateCommentRequest createComment(Long taskId, String message) {
        return new CreateCommentRequest(getAuthHeader(), cookies, taskId, message);
    }

    public EditTaskRequest editTask(Long taskId, List<FieldData> fields) {
        return new EditTaskRequest(getAuthHeader(), cookies, taskId, fields);
    }

    public ChangeStageTaskRequest changeStageTask(Long taskId, Integer targetStageId, List<FieldData> dataList) {
        return new ChangeStageTaskRequest(getAuthHeader(), cookies, taskId, targetStageId, dataList);
    }

    public LogoutRequest close(){
        return new LogoutRequest(getAuthHeader(), cookies);
    }

    public DeleteTaskRequest deleteTask(Long taskId) {
        return new DeleteTaskRequest(getAuthHeader(), cookies, taskId);
    }

    public GetTaskRequest getTask(Long taskId) {
        return new GetTaskRequest(getAuthHeader(), cookies, taskId);
    }

    public static void throwIsUnknownTask(Document document, Long taskId){
        Element element = document.selectFirst("body > center > table > tbody > tr > td > center > h1");
        if(element != null)
            throw new ResponseException("Задача #"+taskId+" не найдена в старом трекере");
    }

    @Data
    public static class FieldData{
        @NonNull
        private Integer id;
        @NonNull
        private TaskFieldOT.Type type;
        @NonNull
        private String data;

        public String getFieldName(){
            switch (type){
                case DATE, DATETIME -> {
                    return "v_field_"+id;
                }
                default -> {
                    return "field_"+id;
                }
            }
        }
    }
}
