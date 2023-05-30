package com.microel.trackerbackend.controllers.telegram.handle;

import com.microel.trackerbackend.storage.entities.team.notification.Notification;

public class Decorator {
    public final static String DOMEN = "http://127.0.0.1:4200/";

    public static String convert(Notification notification) {
        if (notification == null || notification.getMessage() == null || notification.getMessage().isBlank()) {
            return "Пустое уведомление";
        }
        switch (notification.getType()){
            case NEW_COMMENT:
                return "\uD83D\uDCAC " +bold("Комментарий:") + "\n"
                        + parsingLinks(notification.getMessage());
            case TASK_CREATED:
                return "🆕 " +bold("Новая задача:") + "\n"
                        + parsingLinks(notification.getMessage());
            case TASK_EDITED:
                return "✏ " +bold("Изменены детали задачи:") + "\n"
                        + parsingLinks(notification.getMessage());
            case TASK_DELETED:
                return "\uD83D\uDDD1 " +bold("Задача удалена:") + "\n"
                        + parsingLinks(notification.getMessage());
            case TASK_PROCESSED:
                return "\uD83D\uDC77 " +bold("Задача назначена:") + "\n"
                        + parsingLinks(notification.getMessage());
            case TASK_CLOSED:
                return "✅ " +bold("Задача завершена:") + "\n"
                        + parsingLinks(notification.getMessage());
            case TASK_REOPENED:
                return "\uD83C\uDD99 " +bold("Задача возобновлена:") + "\n"
                        + parsingLinks(notification.getMessage());
            case TASK_STAGE_CHANGED:
                return "⏭ " +bold("Стадия задачи изменена:") + "\n"
                        + parsingLinks(notification.getMessage());
            case YOU_RESPONSIBLE:
                return "\uD83D\uDCBC " +bold("Вы ответственный:") + "\n"
                        + parsingLinks(notification.getMessage());
            case YOU_OBSERVER:
                return "\uD83D\uDC41\u200D\uD83D\uDDE8 " +bold("Вы наблюдатель:") + "\n"
                        + parsingLinks(notification.getMessage());
        }
        return "Неизвестный тип уведомления";
    }

    public static String parsingLinks(String text){
        if (text == null) {
            return "";
        }

        return text.replaceAll("\\B#(\\d+)", url("$1", DOMEN+"task/$1"))
                .replaceAll("\\B@([A-z\\d\\-_]+)", url("$1", DOMEN+"employee/$1"))
                .replaceAll("\\B\\$(\\d+)", url("$1", DOMEN+"department/$1"))
                .replaceAll("\\B\\^\\((\\d+)\\)", url("$1", DOMEN+"appointed-installer/$1"));
    }
    public static String bold(String text){
        return "<b>" + text + "</b>";
    }
    public static String italic(String text){
        return "<i>" + text + "</i>";
    }
    public static String underline(String text){
        return "<u>" + text + "</u>";
    }
    public static String strikethrough(String text){
        return "<s>" + text + "</s>";
    }
    public static String spoiler(String text){
        return "<tg-spoiler>" + text + "</tg-spoiler>";
    }
    public static String url(String text, String url){
        return "<a href=\"" + url + "\">" + text + "</a>";
    }
    public static String mention(String text, String id) {
        return "<a href=\"tg://user?id=" + id + "\">" + text + "</a>";
    }
    public static String code(String text){
        return "<code>" + text + "</code>";
    }
    public static String pre(String text){
        return "<pre>" + text + "</pre>";
    }
    public static String preCode(String text){
        return "<pre><code class=\"language-python\">" + text + "</code></pre>";
    }
}
