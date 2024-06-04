package com.microel.trackerbackend.controllers.telegram;

import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    private final List<KeyboardRow> keyboard = new ArrayList<>();
    private final List<List<InlineKeyboardButton>> inlineKeyboard = new ArrayList<>();

    public KeyboardFactory newLine(String... text) {
        final List<InlineKeyboardButton> line = new ArrayList<>();
        final KeyboardRow row = new KeyboardRow();
        for (String s : text) {
            line.add(InlineKeyboardButton.builder().text(s).build());
            row.add(KeyboardButton.builder().text(s).build());
        }
        keyboard.add(row);
        inlineKeyboard.add(line);
        return this;
    }

    public KeyboardFactory newLine(IKButton... buttons) {
        final List<InlineKeyboardButton> line = new ArrayList<>();
        final KeyboardRow row = new KeyboardRow();
        for (IKButton button : buttons) {
            line.add(InlineKeyboardButton.builder().text(button.text).callbackData(button.callbackData).build());
            row.add(KeyboardButton.builder().text(button.text).build());
        }
        keyboard.add(row);
        inlineKeyboard.add(line);
        return this;
    }

    public ReplyKeyboardMarkup getReplyKeyboard() {
        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }

    public InlineKeyboardMarkup getInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(inlineKeyboard)
                .build();
    }

    @Data
    public static class IKButton {
        private String text;
        private String callbackData;

        public static IKButton of(String text, String prefix, String data) {
            final IKButton button = new IKButton();
            button.text = text;
            button.callbackData = CallbackData.create(prefix, data);
            return button;
        }
    }
}
