package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.javatuples.Pair;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.telegram.telegrambots.meta.api.objects.Message;
import reactor.core.publisher.Mono;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Utils {
    public static TlgMessageType getTlgMsgType(Message message) {
        if (message.hasPhoto() || message.hasVideo() || message.hasDocument() || message.hasVoice() ||
                message.hasSticker() || message.hasAnimation() || message.hasVideoNote()
        ) {
            if (message.getMediaGroupId() == null) {
                return TlgMessageType.MEDIA;
            } else {
                return TlgMessageType.GROUP;
            }
        }else{
            return TlgMessageType.TEXT;
        }
    }

    public static String getTlgMsgGroupId(Message message) throws IllegalFields {
        if(message.getChatId() == null)
            throw new IllegalFields("В принятом сообщении отсутствует ChatID");
        if(message.getMediaGroupId() == null)
            throw new IllegalFields("В принятом сообщении отсутствует MediaGroupId");
        return message.getMediaGroupId()+message.getChatId();
    }

    public static Date trimDate(Date date){
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Boolean validIP(@Nullable String ip) {
        if (ip == null || ip.isBlank())
            return false;

        String[] split = ip.split("\\.");
        if (split.length != 4)
            return false;

        for (String s : split){
            try {
                int bt = Integer.parseInt(s);
                if (bt < 0 || bt > 255)
                    return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    public static Pair<Date,Date> getMonthBoundaries(Date date){
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date start = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date end = calendar.getTime();

        return new Pair<>(start,end);
    }


}
