package com.microel.trackerbackend.modules.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.misc.TimeFrame;
import com.microel.trackerbackend.modules.exceptions.DateRangeReadException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class DateRange {

    @Nullable
    private TimeFrame timeFrame;
    @Nullable
    private Timestamp start;
    @Nullable
    private Timestamp end;

    public static DateRange from(String json) throws DateRangeReadException {
        if (json == null || json.isBlank()) return new DateRange();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, DateRange.class);
        } catch (JsonProcessingException e) {
            throw new DateRangeReadException();
        }
    }

    public static DateRange of(Timestamp start, Timestamp end) {
        DateRange dateRange = new DateRange();
        dateRange.start = start;
        dateRange.end = end;
        return dateRange;
    }

    public static DateRange of(TimeFrame timeFrame) {
        DateRange dateRange = new DateRange();
        dateRange.timeFrame = timeFrame;
        return dateRange;
    }

    private static Timestamp getEndNextMonth(){
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.plusMonths(2).withDayOfMonth(1).atStartOfDay());
    }

    private static Timestamp getEndNextWeek(){
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.plusWeeks(2).with(DayOfWeek.MONDAY).atStartOfDay());
    }

    private static Timestamp getEndTomorrow() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.plusDays(2).atStartOfDay());
    }

    private static Timestamp getStartToday() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.atStartOfDay());
    }

    private static Timestamp getEndToday() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.plusDays(1).atStartOfDay());
    }

    private static Timestamp getStartYesterday() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.minusDays(1).atStartOfDay());
    }

    private static Timestamp getStartThisWeek() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.with(DayOfWeek.MONDAY).atStartOfDay());
    }

    private static Timestamp getEndThisWeek() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.plusWeeks(1).with(DayOfWeek.MONDAY).atStartOfDay());
    }

    private static Timestamp getStartLastWeek() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.with(DayOfWeek.MONDAY).minusWeeks(1).atStartOfDay());
    }

    private static Timestamp getEndLastWeek() {
        return getStartThisWeek();
    }

    private static Timestamp getStartThisMonth() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.withDayOfMonth(1).atStartOfDay());
    }

    private static Timestamp getEndThisMonth() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.plusMonths(1).withDayOfMonth(1).atStartOfDay());
    }

    private static Timestamp getStartLastMonth() {
        LocalDate now = LocalDate.now();
        return Timestamp.valueOf(now.withDayOfMonth(1).minusMonths(1).atStartOfDay());
    }

    private static Timestamp getEndLastMonth() {
        return getStartThisMonth();
    }

    public static DateRange nextMonth() {
        return DateRange.of(getEndThisMonth(), getEndNextMonth());
    }

    public static DateRange nextWeek() {
        return DateRange.of(getEndThisWeek(), getEndNextWeek());
    }

    public static DateRange tomorrow() {
        return DateRange.of(getEndToday(), getEndTomorrow());
    }

    public static DateRange today() {
        return DateRange.of(getStartToday(), getEndToday());
    }

    public static DateRange yesterday() {
        return DateRange.of(getStartYesterday(), getStartToday());
    }

    public static DateRange thisWeek() {
        return DateRange.of(getStartThisWeek(), getEndThisWeek());
    }

    public static DateRange lastWeek() {
        return DateRange.of(getStartLastWeek(), getEndLastWeek());
    }

    public static DateRange thisMonth() {
        return DateRange.of(getStartThisMonth(), getEndThisMonth());
    }

    public static DateRange lastMonth() {
        return DateRange.of(getStartLastMonth(), getEndLastMonth());
    }

    public Boolean validate() {
        return (start != null || end != null) || timeFrame != null;
    }

    @Nullable
    public Timestamp start() {
        if (timeFrame != null) {
            switch (timeFrame) {
                case NEXT_MONTH -> {
                    return getEndThisMonth();
                }
                case NEXT_WEEK -> {
                    return getEndThisWeek();
                }
                case TOMORROW -> {
                    return getEndToday();
                }
                case TODAY -> {
                    return getStartToday();
                }
                case YESTERDAY -> {
                    return getStartYesterday();
                }
                case THIS_WEEK -> {
                    return getStartThisWeek();
                }
                case LAST_WEEK -> {
                    return getStartLastWeek();
                }
                case THIS_MONTH -> {
                    return getStartThisMonth();
                }
                case LAST_MONTH -> {
                    return getStartLastMonth();
                }
            }
        }else if(start != null){
            return start;
        }
        return null;
    }

    @Nullable
    public Timestamp end() {
        if (timeFrame != null) {
            switch (timeFrame) {
                case NEXT_MONTH -> {
                    return getEndNextMonth();
                }
                case NEXT_WEEK -> {
                    return getEndNextWeek();
                }
                case TOMORROW -> {
                    return getEndTomorrow();
                }
                case TODAY -> {
                    return getEndToday();
                }
                case YESTERDAY -> {
                    return getStartToday();
                }
                case THIS_WEEK -> {
                    return getEndThisWeek();
                }
                case LAST_WEEK -> {
                    return getEndLastWeek();
                }
                case THIS_MONTH -> {
                    return getEndThisMonth();
                }
                case LAST_MONTH -> {
                    return getEndLastMonth();
                }
            }
        }else if(end != null){
            return end;
        }
        return null;
    }

    public boolean between(Timestamp target) {
        return (target.after(start()) || target.equals(start())) && target.before(end());
    }

    /**
     * Подбирает список TimeFrame по заданному времени
     * @param target Timestamp
     * @return List<TimeFrame>
     */
    public static List<TimeFrame> recognizeTimeFrame(Timestamp target){
        List<TimeFrame> list = new ArrayList<>();
        for (TimeFrame timeFrame : TimeFrame.values()) {
            DateRange dateRange = DateRange.of(timeFrame);
            if(dateRange.between(target)) list.add(timeFrame);
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateRange dateRange)) return false;
        return getTimeFrame() == dateRange.getTimeFrame() && Objects.equals(getStart(), dateRange.getStart()) && Objects.equals(getEnd(), dateRange.getEnd());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTimeFrame(), getStart(), getEnd());
    }

    @Override
    public String toString() {
        return "DateRange{" + "start='" + start + '\'' + ", end='" + end + '\'' + '}';
    }
}
