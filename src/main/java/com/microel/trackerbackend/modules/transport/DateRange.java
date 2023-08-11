package com.microel.trackerbackend.modules.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.modules.exceptions.DateRangeReadException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateRange {
    private String start;
    private String end;

    public Boolean validate(){
        return start != null || end != null;
    }

    public static DateRange from(String json) throws DateRangeReadException {
        if(json == null || json.isBlank()) return new DateRange();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, DateRange.class);
        } catch (JsonProcessingException e) {
            throw new DateRangeReadException();
        }
    }

    public Timestamp getStart() {
        if(start == null || start.isBlank() || start.equals("null")) return null;
        return Timestamp.valueOf(LocalDateTime.parse(start, DateTimeFormatter.ISO_DATE_TIME));
    }

    public Timestamp getEnd() {
        if(end == null || end.isBlank() || end.equals("null")) return null;
        return Timestamp.valueOf(LocalDateTime.parse(end, DateTimeFormatter.ISO_DATE_TIME));
    }

    @Override
    public String toString() {
        return "DateRange{" +
                "start='" + getStart() + '\'' +
                ", end='" + getEnd() + '\'' +
                '}';
    }
}
