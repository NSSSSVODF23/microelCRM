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
public class LogoutRequest implements OldTrackerRequest<Void> {
    @NonNull
    private Map<String,String> headers;
    @NonNull
    private Map<String,String> cookies;

    @Override
    public Void execute() {
        try {
            Jsoup.connect("http://tracker.vdonsk.ru/main.php?mode=logout").headers(headers).cookies(cookies).method(Connection.Method.GET).execute();
            return null;
        } catch (IOException e) {
            throw new ResponseException(e.getMessage());
        }
    }
}
