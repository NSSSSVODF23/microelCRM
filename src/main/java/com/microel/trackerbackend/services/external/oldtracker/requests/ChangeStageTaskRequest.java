package com.microel.trackerbackend.services.external.oldtracker.requests;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequest;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequestFactory;
import lombok.Data;
import lombok.NonNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ChangeStageTaskRequest implements OldTrackerRequest<Void> {
    @NonNull
    private Map<String,String> headers;
    @NonNull
    private Map<String,String> cookies;
    @NonNull
    private Long taskId;
    @NonNull
    private Integer targetStageId;
    @NonNull
    private List<OldTrackerRequestFactory.FieldData> fields;

    @Override
    public Void execute() {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("status", targetStageId.toString());
            body.put("force_save", "1");
            body.put("obji_action", "0");
            body.put("doc_id", "0");
            Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=show_obji&obji="+taskId+"&from_cat=1")
                    .headers(headers).cookies(cookies).data(body).method(Connection.Method.POST).execute();
            for (OldTrackerRequestFactory.FieldData field : fields) {
                body.put(field.getFieldName(), field.getData());
            }
            Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=show_obji&obji="+taskId+"&from_cat=1")
                    .headers(headers).cookies(cookies).data(body).method(Connection.Method.POST).execute();
            return null;
        } catch (IOException e) {
            throw new ResponseException(e.getMessage());
        }
    }
}
