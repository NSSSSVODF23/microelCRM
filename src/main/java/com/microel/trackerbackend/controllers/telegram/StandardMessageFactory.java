package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@AllArgsConstructor
public class StandardMessageFactory {

    private String chatId;

    public static StandardMessageFactory create(Long chatId) throws Exception {
        if(chatId == null) throw new Exception("Пустой chatId");
        return new StandardMessageFactory(String.valueOf(chatId));
    }

    public static StandardMessageFactory create(String chatId) throws Exception {
        if(chatId == null) throw new Exception("Пустой chatId");
        return new StandardMessageFactory(chatId);
    }

    public SendMessage userIdResponse(){
        return new SendMessage(chatId, "Ваш TelegramId: " + chatId + "\n Введите его в настройках приложения, чтобы получать сообщения.");
    }

    public SendMessage acceptWorkLog(Task task, Employee employee) {
        String employeeName = employee.getLastName() + " " + employee.getFirstName();
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder
                .append("\uD83D\uDC77\u200D♂️ ")
                .append(MessageConverter.bold("Задача #" + task.getTaskId() + " в работе ")).append("\n")
                .append(MessageConverter.mention(employeeName, employee.getTelegramUserId()))
                .append(" назначил вас на выполнение задачи");

        KeyboardRow acceptKeyboardRow = new KeyboardRow();
        acceptKeyboardRow.add(KeyboardButton.builder()
                .text("Принять задачу")
                .build());
        return SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder.toString())
                .parseMode("HTML")
                .replyMarkup(ReplyKeyboardMarkup.builder()
                        .oneTimeKeyboard(true)
                        .keyboardRow(acceptKeyboardRow)
                        .build())
                .build();
    }

    public SendMessage unknownCommand() {
        return new SendMessage(chatId, "Команда не распознана. Попробуйте еще раз.");
    }
}
