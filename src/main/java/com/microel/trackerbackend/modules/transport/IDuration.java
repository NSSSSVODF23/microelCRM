package com.microel.trackerbackend.modules.transport;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class IDuration {
    private Integer years;
    private Integer months;
    private Integer days;
    private Integer milliseconds;
    private Boolean specifiedWeeks;

    public Timestamp shift(Timestamp timestamp){
        Instant instant = timestamp.toInstant();
        // Складываем значения и вычисляем кол-во миллисекунд
        long milliseconds = this.milliseconds + (this.days * 86400000) + (this.months * 2592000000L) + (this.years * 31536000000L);
        Instant shifted = instant.plusMillis(milliseconds);
        return Timestamp.from(shifted);
    }
}
