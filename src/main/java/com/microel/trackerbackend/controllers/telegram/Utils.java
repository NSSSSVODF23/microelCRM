package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.telegram.telegrambots.meta.api.objects.Message;

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
}
