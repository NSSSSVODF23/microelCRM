package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class TelegramController {
    @Nullable
    private TelegramBotsApi api;
    @Nullable
    private MainBot mainBot;
    @Nullable
    private BotSession mainBotSession;
    private final ConfigurationStorage configurationStorage;

    public TelegramController(ConfigurationStorage configurationStorage) {
        this.configurationStorage = configurationStorage;
        try {
            TelegramConf telegramConf = configurationStorage.load(TelegramConf.class);
            initializeMainBot(telegramConf);
        } catch (FailedToReadConfigurationException e) {
            log.warn("Конфигурация для Telegram не найдена");
        } catch (TelegramApiException e) {
            log.error("Ошибка Telegram API "+e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void initializeApi() throws TelegramApiException {
        if(api == null) api = new TelegramBotsApi(DefaultBotSession.class);
    }

    private void initializeMainBot(TelegramConf configuration) throws TelegramApiException, IOException {
        initializeApi();
        if(api == null) throw new IOException("Telegram API не инициализировано");
        if(mainBotSession != null) mainBotSession.stop();
        mainBot = new MainBot(configuration);
        mainBot.subscribe(new TelegramCommandReactor("/start", update -> {
            Message  message = update.getMessage();
            try {
                mainBot.execute(StandardMessageFactory.create(message.getChatId()).userIdResponse());
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }));
        mainBotSession = api.registerBot(mainBot);
    }

    public void changeTelegramConf(TelegramConf telegramConf) throws FailedToWriteConfigurationException, TelegramApiException, IOException {
        configurationStorage.save(telegramConf);
        initializeMainBot(telegramConf);
    }

    public void sendNotification(Employee employee, Notification notification){
        if(mainBot == null) {
            log.warn("Попытка отправить уведомление при не инициализированном TelegramApi");
            return;
        }
        if(employee.getTelegramUserId() == null || notification.getMessage() == null) return;
        SendMessage sendMessage = SendMessage.builder()
                .chatId(employee.getTelegramUserId())
                .parseMode("HTML")
                .text(MessageConverter.convert(notification))
                .build();
        try {
            mainBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить уведомление "+notification.getType()+":"+e.getMessage());
        }
    }

    public void broadcastChatMessage(Chat chat, ChatMessage chatMessage) {
        if(mainBot == null) {
            log.warn("Попытка broadcastMessage при не инициализированном TelegramApi");
            return;
        }

        for(Employee employee: chat.getMembers()){
            if(employee.getTelegramUserId() == null || employee.equals(chatMessage.getAuthor())) continue;
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(employee.getTelegramUserId())
                    .parseMode("HTML")// TODO Тут еще реализовать отправку файлов
                    .text(chatMessage.getText())
                    .build();
            try {
                mainBot.execute(sendMessage);
            } catch (TelegramApiException e) {
                log.warn("Сообщение не отправлено "+e.getMessage());
            }
        }
    }

    public void assignInstallers(Task task, Set<Employee> installers, Employee employee) {
        if(mainBot == null) {
            log.warn("Попытка assignInstallers при не инициализированном TelegramApi");
            return;
        }
        for(Employee installer: installers){
            if(installer.getTelegramUserId() == null || installer.getTelegramUserId().isBlank()) continue;
            try {
                mainBot.execute(StandardMessageFactory.create(installer.getTelegramUserId()).acceptWorkLog(task, employee));
                mainBot.subscribe(new TelegramPromptReactor(update -> {
                    if(update.getMessage().getText().equals("Принять задачу")){
                        System.out.println("Задача принята");
                    }
                }), true);
            } catch (Exception e) {
                log.warn("Сообщение не отправлено "+e.getMessage());
            }
        }
    }
}
