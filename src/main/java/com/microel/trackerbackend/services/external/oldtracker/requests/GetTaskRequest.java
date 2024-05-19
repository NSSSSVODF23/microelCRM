package com.microel.trackerbackend.services.external.oldtracker.requests;

import com.microel.trackerbackend.parsers.oldtracker.OldTrackerTaskFactory;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequest;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequestFactory;
import lombok.Data;
import lombok.NonNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class GetTaskRequest implements OldTrackerRequest<GetTaskRequest.TaskInfo> {
    @NonNull
    private Map<String,String> headers;
    @NonNull
    private Map<String,String> cookies;
    @NonNull
    private Long taskId;

    @Override
    public TaskInfo execute() {
        try {
            Document document = Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=show_obji&obji=" + taskId + "&from_cat=1")
                    .headers(headers).cookies(cookies).maxBodySize(0).method(Connection.Method.GET).timeout(15000).execute().bufferUp().parse();

            OldTrackerRequestFactory.throwIsUnknownTask(document, taskId);

            Element titleWithClassName = document.selectFirst("#form > center > div > table > tbody > tr.titleTr > td");
            Pattern classNamePattern = Pattern.compile("([\\s\\S]+) #\\d+", Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
            if(titleWithClassName == null) throw new ResponseException("Не удалось определить класс задачи #"+taskId);
            Matcher classNameMatcher = classNamePattern.matcher(titleWithClassName.text());

            if(!classNameMatcher.find()) throw new ResponseException("Не удалось распознать класс задачи #"+taskId);
            String taskClassName = classNameMatcher.group(1);

            Element stageElement = document.selectFirst("#status > option[selected]");
            if(stageElement == null) throw new ResponseException("Не удалось определить стадию задачи #"+taskId);
            String taskStageId = stageElement.val();
            if(taskStageId.isBlank()) throw new ResponseException("Не удалось определить стадию задачи #"+taskId);
            return new TaskInfo(taskClassName, Integer.parseInt(taskStageId));
        } catch (Exception e) {
            System.out.println("Не удалось получить задачу из старого трекера");
        }
        return null;
    }

    @Data
    public static class TaskInfo{
        @NonNull
        private String className;
        @NonNull
        private Integer stageId;
    }
}
