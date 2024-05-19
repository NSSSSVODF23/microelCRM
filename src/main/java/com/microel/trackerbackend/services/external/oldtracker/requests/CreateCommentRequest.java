package com.microel.trackerbackend.services.external.oldtracker.requests;

import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequest;
import lombok.Data;
import lombok.NonNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
public class CreateCommentRequest implements OldTrackerRequest<Void> {
    @NonNull
    private Map<String, String> headers;
    @NonNull
    private Map<String, String> cookies;
    @NonNull
    private Long taskId;
    @NonNull
    private String message;

    @Override
    public Void execute() {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("mcomments_new_entry", message);
            body.put("force_save", "1");
            body.put("obji_action", "0");
            body.put("doc_id", "0");
            Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=show_obji&obji=" + taskId + "&from_cat=1")
                    .headers(headers).cookies(cookies).data(body).method(Connection.Method.POST).timeout(15000).execute();
        } catch (Exception e) {
            System.out.println("Не удалось создать комментарий");
        }
        return null;
    }
}
