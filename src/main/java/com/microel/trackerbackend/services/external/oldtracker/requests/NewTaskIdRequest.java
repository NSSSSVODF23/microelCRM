package com.microel.trackerbackend.services.external.oldtracker.requests;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequest;
import lombok.Data;
import lombok.NonNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class NewTaskIdRequest implements OldTrackerRequest<Long> {
    @NonNull
    private Map<String,String> headers;
    @NonNull
    private Map<String,String> cookies;
    @NonNull
    private Integer taskClassId;

    @Override
    public Long execute() {
        try {
            Document request = Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=create_obji&obj_sel=" + taskClassId + "&from_cat=1").headers(headers).cookies(cookies).post();
            String attrValue = request.getElementsByTag("META").get(0).attr("CONTENT");
            Pattern pattern = Pattern.compile("main\\.php\\?mode=show_obji&obji=(\\d+)");
            Matcher matcher = pattern.matcher(attrValue);
            if(matcher.find()){
                return Long.parseLong(matcher.group(1));
            }else{
                throw new ResponseException("Не удалось получить новый идентификатор задачи в старом трекере");
            }
        } catch (IOException e) {
            throw new ResponseException(e.getMessage());
        }
    }
}
