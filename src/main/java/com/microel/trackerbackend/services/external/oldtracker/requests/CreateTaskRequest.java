package com.microel.trackerbackend.services.external.oldtracker.requests;

import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequest;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequestFactory;
import lombok.Data;
import lombok.NonNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
public class CreateTaskRequest implements OldTrackerRequest<Void> {
    @NonNull
    private Map<String, String> headers;
    @NonNull
    private Map<String, String> cookies;
    @NonNull
    private Long taskId;
    @NonNull
    private Integer stageId;
    @NonNull
    private List<OldTrackerRequestFactory.FieldData> fields;
    private String initialComment;

    public CreateTaskRequest setInitialComment(String initialComment) {
        this.initialComment = initialComment;
        return this;
    }

    @Override
    public Void execute() {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("status", stageId.toString());
            body.put("force_save", "1");
            body.put("obji_action", "0");
            body.put("doc_id", "0");
            if (initialComment != null && !initialComment.isBlank())
                body.put("mcomments_new_entry", initialComment);
            for (OldTrackerRequestFactory.FieldData field : fields) {
                body.put(field.getFieldName(), field.getData());
            }
            Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=show_obji&obji=" + taskId + "&from_cat=1")
                    .headers(headers).cookies(cookies).data(body).method(Connection.Method.POST).timeout(15000).execute();
        } catch (Exception e) {
            System.out.println("Не удалось создать задачу в старом трекере");
        }
        return null;
    }
}
