package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.controllers.telegram.handle.Decorator;
import com.microel.trackerbackend.controllers.telegram.reactor.*;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.dto.mapper.ChatMapper;
import com.microel.trackerbackend.storage.dto.task.WorkLogDto;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.*;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.AttachmentType;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;

@Slf4j
@Component
@Transactional
public class TelegramController {
    private final ConfigurationStorage configurationStorage;
    private final TaskDispatcher taskDispatcher;
    private final WorkLogDispatcher workLogDispatcher;
    private final ChatDispatcher chatDispatcher;
    private final ChatMessageDispatcher chatMessageDispatcher;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;
    private final AttachmentDispatcher attachmentDispatcher;
    private final Map<String, List<Message>> groupedMessagesFromTelegram = new ConcurrentHashMap<>();
    private final Map<Long, List<Message>> chatsInTaskCloseMode = new ConcurrentHashMap<>();
    @Nullable
    private TelegramBotsApi api;
    @Nullable
    private MainBot mainBot;
    @Nullable
    private BotSession mainBotSession;

    public TelegramController(ConfigurationStorage configurationStorage, TaskDispatcher taskDispatcher, WorkLogDispatcher workLogDispatcher,
                              ChatDispatcher chatDispatcher, ChatMessageDispatcher chatMessageDispatcher, EmployeeDispatcher employeeDispatcher,
                              StompController stompController, AttachmentDispatcher attachmentDispatcher) {
        this.configurationStorage = configurationStorage;
        this.taskDispatcher = taskDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.chatDispatcher = chatDispatcher;
        this.chatMessageDispatcher = chatMessageDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
        this.attachmentDispatcher = attachmentDispatcher;
        try {
            TelegramConf telegramConf = configurationStorage.load(TelegramConf.class);
            initializeMainBot(telegramConf);
        } catch (FailedToReadConfigurationException e) {
            log.warn("Конфигурация для Telegram не найдена");
        } catch (TelegramApiException e) {
            log.error("Ошибка Telegram API " + e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void initializeApi() throws TelegramApiException {
        if (api == null) api = new TelegramBotsApi(DefaultBotSession.class);
    }

    public void initializeMainBot(TelegramConf configuration) throws TelegramApiException, IOException {
        initializeApi();
        if (api == null) throw new IOException("Telegram API не инициализировано");
        if (mainBotSession != null) mainBotSession.stop();
        mainBot = new MainBot(configuration);
        initializeChatCommands();

        mainBotSession = api.registerBot(mainBot);
    }

    private void initializeChatCommands() throws IOException, TelegramApiException {
        if (mainBot == null) throw new IOException("Telegram Bot не инициализирован");
        List<BotCommand> commands = List.of(
                new BotCommand("menu", "Основные действия")
        );
        SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(commands)
                .build();
        mainBot.execute(setMyCommands);

        mainBot.subscribe(new TelegramCommandReactor("/start", update -> {
            Message message = update.getMessage();
            TelegramMessageFactory.create(message.getChatId(), mainBot).userIdResponse().execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCommandReactor("/mytasks", update -> {
            Long chatId = update.getMessage().getChatId();
            return true;
        }));

        mainBot.subscribe(new TelegramCommandReactor("/menu", update -> {
            Long chatId = update.getMessage().getChatId();
            Employee employee = employeeDispatcher.getByTelegramId(chatId).orElse(null);
            if (employee == null) return false;
            if (employee.getOffsite()) {
                TelegramMessageFactory.create(chatId, mainBot).offsiteMenu().execute();
            } else {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage("Меню отсутствует").execute();
            }
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("main_menu", (u, data) -> {
            if (data == null) return false;
            Long chatId = u.getCallbackQuery().getMessage().getChatId();
            Integer messageId = u.getCallbackQuery().getMessage().getMessageId();
            if (data.isData("active_task")) {
                mainBot.send(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                WorkLogDto acceptedByTelegramId = workLogDispatcher.getAcceptedByTelegramId(chatId);
                TelegramMessageFactory.create(chatId, mainBot).currentActiveTask(acceptedByTelegramId.getTask()).execute();
                return true;
            } else if (data.isData("tasks_queue")) {
                TelegramMessageFactory.create(chatId, mainBot).deleteMessage(messageId).execute();
                return sendWorkLogQueue(chatId);
            }
            return false;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("accept_work_log", (update, data) -> {
            if (data == null) return false;
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            EditMessageReplyMarkup clearMarkup = EditMessageReplyMarkup.builder().chatId(chatId).messageId(messageId)
                    .replyMarkup(InlineKeyboardMarkup.builder().clearKeyboard().build()).build();
            mainBot.send(clearMarkup);
            ChatDto chatFromWorkLog = workLogDispatcher.getChatByWorkLogId(data.getLong());
            Employee employee = employeeDispatcher.getByTelegramId(chatId).orElseThrow(() -> new EntryNotFound("Пользователь не найден по идентификатору Telegram Api"));
            SuperMessage systemMessage = chatDispatcher.createSystemMessage(chatFromWorkLog.getChatId(), "\uD83D\uDC77\uD83C\uDFFB\u200D♂️" + employee.getFullName() + " принял задачу и подключился к чату", this);
            WorkLogDto workLog = workLogDispatcher.acceptWorkLog(data.getLong(), chatId);
            // Отправляем обновления в интерфейс пользователя
            broadcastUpdatesToWeb(systemMessage);
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            messageFactory.currentActiveTask(workLog.getTask()).execute();
            if (workLog.getTargetDescription() != null && !workLog.getTargetDescription().isBlank())
                messageFactory.simpleMessage("Текущая цель:\n" + workLog.getTargetDescription()).execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("send_report", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            if (!chatsInTaskCloseMode.containsKey(chatId)) {
                factory.deleteMessage(messageId).execute();
                factory.simpleMessage("\uD83D\uDEAB Вы не можете отправить отчет, так как нет активной задачи.")
                        .execute();
                return true;
            } else if (chatsInTaskCloseMode.get(chatId).isEmpty()) {
                factory.simpleMessage("Перед закрытием нужно отправить сообщение (или несколько), с отчетом. Отчет о выполненных работах не может быть пуст.")
                        .execute();
                return false;
            }
            try {
                List<Message> messageList = chatsInTaskCloseMode.get(chatId);
                WorkLog workLog = workLogDispatcher.createReport(chatId, messageList);
                chatsInTaskCloseMode.remove(chatId);
                factory.deleteMessage(messageId).execute();
                factory.simpleMessage("Отчет успешно отправлен").execute();
                Employee employee = employeeDispatcher.getByTelegramId(chatId).orElseThrow(() -> new EntryNotFound("Сотрудник не найден"));
                sendTextBroadcastMessage(workLog.getChat(), ChatMessage.of("Написал отчет и отключился от чата задачи.", employee));
                return sendWorkLogQueue(chatId);
            } catch (EntryNotFound | IllegalFields e) {
                factory.deleteMessage(messageId).execute();
                factory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("cancel_close", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            mainBot.send(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
            if (chatsInTaskCloseMode.containsKey(chatId)) {
                chatsInTaskCloseMode.remove(chatId);
                WorkLogDto workLog = workLogDispatcher.getAcceptedByTelegramId(chatId);
                TelegramMessageFactory.create(chatId, mainBot).currentActiveTask(workLog.getTask()).execute();
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage("Вы отменили завершение задачи, она вновь активна. Вы находитесь в режиме чата задачи.").execute();
                return true;
            }
            TelegramMessageFactory.create(chatId, mainBot).simpleMessage("Вы не можете отменить завершение задачи так как находитесь в режиме чата.").execute();
            return false;
        }));

        mainBot.subscribe(new TelegramPromptReactor("\uD83D\uDC4C Завершить задачу", (update) -> {
            // Получаем активную задачу по идентификатору чата
            Long chatId = update.getMessage().getChatId();
            try {
                WorkLogDto workLog = workLogDispatcher.getAcceptedByTelegramId(chatId);
                TelegramMessageFactory.create(chatId, mainBot).closeWorkLogMessage().execute();
                TelegramMessageFactory.create(chatId, mainBot).clearKeyboardMenu().execute();
                chatsInTaskCloseMode.put(chatId, new ArrayList<>());
                return true;
            } catch (EntryNotFound e) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage("В данный момент у вас нет активных задач").execute();
                TelegramMessageFactory.create(chatId, mainBot).clearKeyboardMenu().execute();
            }
            return false;
        }));

        // Обработка простых текстовых сообщений полученных из чата
        mainBot.subscribe(new TelegramMessageReactor(update -> {
            Long chatId = update.getMessage().getChatId();
            if (chatsInTaskCloseMode.containsKey(chatId)) {
                chatsInTaskCloseMode.get(chatId).add(update.getMessage());
                return true;
            }
            try {
                sendMessageFromTlgChat(update.getMessage());
                return true;
            } catch (IllegalFields | SaveEntryFailed | IllegalMediaType | ExceptionInsideThread e) {
                log.warn(e.getMessage());
            }
            return false;
        }));

        mainBot.subscribe(new TelegramEditMessageReactor(update -> {
            Long chatId = update.getEditedMessage().getChatId();
            if (chatsInTaskCloseMode.containsKey(chatId)) {
                    Message updateMessage = update.getEditedMessage();
                    chatsInTaskCloseMode.put(chatId,
                            chatsInTaskCloseMode.get(chatId).stream().map(message -> {
                                if (Objects.equals(message.getMessageId(), updateMessage.getMessageId())) {
                                    return updateMessage;
                                }
                                return message;
                            }).collect(Collectors.toList())
                    );
                return true;
            }
            Message editedMessage = update.getEditedMessage();
            updateMessageFromTlgChat(editedMessage);
            return true;
        }));
    }

    private boolean sendWorkLogQueue(Long chatId) throws TelegramApiException {
        List<WorkLogDto> notAcceptedWorkLogs = new ArrayList<>();
        AtomicBoolean hasActive = new AtomicBoolean(false);
        List<WorkLogDto> queueByTelegramId = workLogDispatcher.getQueueByTelegramId(chatId);
        for (WorkLogDto workLog : queueByTelegramId) {
            boolean isAccepted = workLog.getAcceptedEmployees().stream().anyMatch(e -> Objects.equals(e.getTelegramUserId(), chatId.toString()));
            if (isAccepted) {
                TelegramMessageFactory.create(chatId, mainBot).workLogListItem(workLog, true, false, null).execute();
                hasActive.set(true);
            } else {
                notAcceptedWorkLogs.add(workLog);
            }
        }
        if (!notAcceptedWorkLogs.isEmpty()) {
            TelegramMessageFactory.create(chatId, mainBot).simpleMessage(Decorator.underline("Список задач назначенных вам:")).execute();
        } else {
            if (!hasActive.get())
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage("Задачи пока вам не назначены").execute();
        }
        AtomicInteger i = new AtomicInteger(0);
        List<TelegramMessageFactory.AbstractExecutor<Message>> messages = notAcceptedWorkLogs.stream().map((wl) -> TelegramMessageFactory.create(chatId, mainBot).workLogListItem(wl, false, !hasActive.get(), i.incrementAndGet())).collect(Collectors.toList());
        for (TelegramMessageFactory.AbstractExecutor<Message> messageExecutor : messages) {
            messageExecutor.execute();
        }
        return true;
    }

    /**
     * Находит вложения в сообщении из telegram api, сохраняет их локально, и добавляет {@link Attachment} в базу данных.
     *
     * @param message {@link Message} от telegram api
     * @return {@link Attachment} или null если вложения в сообщении не было.
     */
    @Nullable
    public Attachment getAttachmentFromMessage(Message message) {
        try {
            if (message.hasPhoto()) {
                PhotoSize bigPhoto = message.getPhoto().stream().max(Comparator.comparingInt(a -> (a.getWidth() + a.getHeight()))).orElse(null);
                if (bigPhoto != null) {
                    byte[] fileBytes = downloadFile(bigPhoto.getFileId());
                    return attachmentDispatcher.saveAttachment(FileData.of(bigPhoto.getFileUniqueId() + ".jpg", "image/jpeg", fileBytes));
                } else {
                    return null;
                }
            }
            if (message.hasVideo()) {
                Video video = message.getVideo();
                byte[] fileBytes = downloadFile(video.getFileId());
                if (fileBytes == null) return null;
                if (video.getFileName() != null && video.getMimeType() != null) {
                    return attachmentDispatcher.saveAttachment(FileData.of(video.getFileName(), video.getMimeType(), fileBytes));
                } else {
                    return attachmentDispatcher.saveAttachment(FileData.of(video.getFileUniqueId() + ".mp4", "video/mp4", fileBytes));
                }
            }
            if (message.hasDocument()) {
                Document document = message.getDocument();
                byte[] fileBytes = downloadFile(document.getFileId());
                if (fileBytes == null) return null;
                if (document.getFileName() != null && document.getMimeType() != null) {
                    return attachmentDispatcher.saveAttachment(FileData.of(document.getFileName(), document.getMimeType(), fileBytes));
                } else {
                    return attachmentDispatcher.saveAttachment(FileData.of(document.getFileUniqueId() + ".txt", "text/plain", fileBytes));
                }
            }
            if (message.hasAudio()) {
                Audio audio = message.getAudio();
                byte[] fileBytes = downloadFile(audio.getFileId());
                if (fileBytes == null) return null;
                if (audio.getFileName() != null && audio.getMimeType() != null) {
                    return attachmentDispatcher.saveAttachment(FileData.of(audio.getFileName(), audio.getMimeType(), fileBytes));
                } else {
                    return attachmentDispatcher.saveAttachment(FileData.of(audio.getFileUniqueId() + ".ogg", "audio/ogg", fileBytes));
                }
            }
            if (message.hasVoice()) {
                Voice voice = message.getVoice();
                byte[] fileBytes = downloadFile(voice.getFileId());
                if (fileBytes == null) return null;
                return attachmentDispatcher.saveAttachment(FileData.of(voice.getFileUniqueId() + ".ogg", "audio/ogg", fileBytes));
            }
            if (message.hasVideoNote()) {
                VideoNote videoNote = message.getVideoNote();
                byte[] fileBytes = downloadFile(videoNote.getFileId());
                if (fileBytes == null) return null;
                return attachmentDispatcher.saveAttachment(FileData.of(videoNote.getFileUniqueId() + ".mp4", "video/mp4", fileBytes));
            }
            if (message.hasSticker()) {
                Sticker sticker = message.getSticker();
                byte[] fileBytes = downloadFile(sticker.getFileId());
                if (fileBytes == null) return null;
                if ((sticker.getIsAnimated() || sticker.getIsVideo()) && sticker.getThumb() != null) {
                    byte[] thumbBytes = downloadFile(sticker.getThumb().getFileId());
                    if (thumbBytes == null) return null;
                    return attachmentDispatcher.saveAttachment(FileData.of(sticker.getThumb().getFileUniqueId() + ".webp", "image/webp", thumbBytes));
                } else {
                    return attachmentDispatcher.saveAttachment(FileData.of(sticker.getFileUniqueId() + ".png", "image/png", fileBytes));
                }
            }
            return null;
        } catch (WriteError | EmptyFile error) {
            log.warn("Не удалось сохранить файл " + error.getMessage());
            return null;
        }
    }

    @Nullable
    private byte[] downloadFile(String fileId) {
        if (mainBot == null) {
            log.warn("Попытка скачать файл при не инициализированном TelegramApi");
            return null;
        }
        try {
            GetFile file = GetFile.builder().fileId(fileId).build();
            File execute = mainBot.execute(file);
            try (InputStream in = new URL(execute.getFileUrl(mainBot.getToken())).openStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                log.warn("Не удалось скачать файл " + execute.getFilePath() + " " + e.getMessage());
                return null;
            }
        } catch (TelegramApiException e) {
            log.warn("Ошибка скачивания файла telegram api: " + e.getMessage());
            return null;
        }
    }

    public void changeTelegramConf(TelegramConf telegramConf) throws
            FailedToWriteConfigurationException, TelegramApiException, IOException {
        configurationStorage.save(telegramConf);
        initializeMainBot(telegramConf);
    }

    public void sendNotification(Employee employee, Notification notification) {
        if (mainBot == null) {
            log.warn("Попытка отправить уведомление при не инициализированном TelegramApi");
            return;
        }
        if (employee.getTelegramUserId() == null || notification.getMessage() == null) return;
        SendMessage sendMessage = SendMessage.builder()
                .chatId(employee.getTelegramUserId())
                .parseMode("HTML")
                .text(Decorator.convert(notification))
                .build();
        try {
            mainBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить уведомление " + notification.getType() + ":" + e.getMessage());
        }
    }

    private void broadcastEditChatMessage(ChatDto chat, ChatMessageDto chatMessage) {

    }

    public void refreshChatUnreadCount(Chat chat, SuperMessage superMessage) {
        chat.getMembers()
                .stream()
                .filter(e -> !e.getLogin().equals(superMessage.getAuthor().getLogin()))
                .forEach(e -> {
                    String login = e.getLogin();
                    Long chatId = chat.getChatId();
                    Long countUnreadMessages = chatDispatcher.getUnreadMessagesCount(chatId, e);
                    stompController.updateCountUnreadMessage(login, chatId, countUnreadMessages);
                });
    }

    /**
     * Обрабатывает сообщения полученные из web версии crm, и отправляет их всем пользователям чата
     *
     * @param chatId      Идентификатор чата из базы данных
     * @param messageData Данные сообщения полученные из web
     * @param author      Объект сотрудника отправившего сообщение
     * @throws EntryNotFound        Если не находит чат или сообщение в базе данных
     * @throws EmptyFile            Попытка сохранить пустой файл
     * @throws WriteError           Ошибка при записи файла на диск
     * @throws TelegramApiException Ошибка при обращении к telegram api
     * @throws IllegalFields        Ошибка при попытке отправить мультимедиа-сообщение без вложения
     * @throws IllegalMediaType     Ошибка при неудачной попытке определить тип данных вложения
     */
    public void sendMessageFromWeb(Long chatId, MessageData messageData, Employee author) throws EntryNotFound,
            EmptyFile, WriteError, TelegramApiException, IllegalFields, IllegalMediaType {
        // Находим целевой чат
        Chat chat = chatDispatcher.getChat(chatId);

        if (chat.getClosed() != null) {
            throw new IllegalFields("Не возможно отправить сообщение в закрытый чат");
        }

        if (!chat.getMembers().contains(author)) {
            chat.getMembers().add(author);
            List<SuperMessage> superMessages = chatDispatcher.setAllMessagesAsRead(chat.getChatId(), author);
            superMessages.forEach(stompController::updateMessage);
            stompController.updateChat(chatDispatcher.unsafeSave(chat));
            stompController.updateCountUnreadMessage(author.getLogin(), chatId, 0L);
        }

        // Из messageData создаем одно или несколько сообщений в зависимости от вложений
        Set<Attachment> attachments = new HashSet<>();
        if (!messageData.getFiles().isEmpty()) {
            attachments = new HashSet<>(attachmentDispatcher.saveAttachments(messageData.getFiles()));
        }

        if (attachments.size() > 1) {
            // Если вложений больше 1 создаем несколько сообщений по медиа группам для корректной отправки в tg


            List<Attachment> visualMedia = attachments.stream()
                    .filter(a -> a.getType() == AttachmentType.PHOTO || a.getType() == AttachmentType.VIDEO)
                    .collect(Collectors.toList());
            List<Attachment> audioMedia = attachments.stream()
                    .filter(a -> a.getType() == AttachmentType.AUDIO)
                    .collect(Collectors.toList());
            List<Attachment> documentsMedia = attachments.stream()
                    .filter(a -> a.getType() == AttachmentType.DOCUMENT || a.getType() == AttachmentType.FILE)
                    .collect(Collectors.toList());

            if (messageData.getText() != null && !messageData.getText().isBlank()) {
                // Создаем текстовое сообщение, если есть текст

                // Записываем в бд и транслируем в tg чаты членов чата
                SuperMessage textMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, messageData.getReplyMessageId(), this);

                // Обновляем web
                broadcastUpdatesToWeb(textMessage);
            } else {
                // Если текста нет, транслируем в чаты метку отправителя

                sendTextBroadcastMessage(chat, ChatMessage.of("", author));
            }

            // Создаем, отправляем и транслируем медиа сообщения в зависимости от количества вложений
            if (visualMedia.size() == 1) {
                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, visualMedia.get(0), messageData.getReplyMessageId(), this);

                // Отправляем обновления в интерфейс пользователя
                broadcastUpdatesToWeb(mediaMessage);
            } else if (visualMedia.size() > 1) {
                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, visualMedia, messageData.getReplyMessageId(), this);

                // Отправляем обновления в интерфейс пользователя
                broadcastUpdatesToWeb(mediaMessage);
            }
            if (audioMedia.size() == 1) {
                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, audioMedia.get(0), messageData.getReplyMessageId(), this);

                // Отправляем обновления в интерфейс пользователя
                broadcastUpdatesToWeb(mediaMessage);
            } else if (audioMedia.size() > 1) {
                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, audioMedia, messageData.getReplyMessageId(), this);

                // Отправляем обновления в интерфейс пользователя
                broadcastUpdatesToWeb(mediaMessage);
            }
            if (documentsMedia.size() == 1) {
                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, documentsMedia.get(0), messageData.getReplyMessageId(), this);

                // Отправляем обновления в интерфейс пользователя
                broadcastUpdatesToWeb(mediaMessage);
            } else if (documentsMedia.size() > 1) {
                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, documentsMedia, messageData.getReplyMessageId(), this);

                // Отправляем обновления в интерфейс пользователя
                broadcastUpdatesToWeb(mediaMessage);
            }

        } else if (attachments.size() == 1) {
            // Если вложение одно, то создаем простое медиа сообщение

            Attachment attachment = attachments.iterator().next();

            SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, attachment, messageData.getReplyMessageId(), this);

            // Отправляем обновления в интерфейс пользователя
            broadcastUpdatesToWeb(mediaMessage);
        } else {
            // Если вложений нет, то создаем текстовое сообщение.

            // Записываем в бд и транслируем в tg чаты членов чата
            SuperMessage textMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, messageData.getReplyMessageId(), this);

            // Обновляем web
            broadcastUpdatesToWeb(textMessage);
        }
    }

    /**
     * Удаляет сообщение из базы данных и из telegram api при запросе от web crm
     *
     * @param messageId Идентификатор целевого сообщения
     * @param employee  Кто удаляет сообщение
     * @return Список удаленных сообщений
     * @throws EntryNotFound  Если сообщение не найдено
     * @throws NotOwner       Если сообщение не принадлежит сотруднику
     * @throws AlreadyDeleted Если сообщение уже удалено
     */
    public SuperMessage deleteMessageFromWeb(Long messageId, Employee employee) throws EntryNotFound, NotOwner, AlreadyDeleted, TelegramApiException {
        List<ChatMessage> listOfRelatedMessages = chatDispatcher.getListOfRelatedMessages(messageId);
        ChatMessageMediaGroup listOfDeletedMessages = new ChatMessageMediaGroup();
        for (ChatMessage chatMessage : listOfRelatedMessages) {
            listOfDeletedMessages.addMessage(chatDispatcher.deleteMessage(chatMessage.getChatMessageId(), employee));
            deleteMessageFromTelegram(chatMessage);
        }
        SuperMessage superMessage = listOfDeletedMessages.getSuperMessage();
        stompController.deleteMessage(superMessage);
        return superMessage;
    }


    /**
     * Редактирует сообщение из базы данных и из telegram api при запросе от web crm
     *
     * @param editMessageId Идентификатор редактируемого сообщения
     * @param text          Отредактированный текст
     * @param author        Кто редактирует сообщение
     * @throws TelegramApiException Если ошибка при отправке сообщения в telegram api
     * @throws EntryNotFound        Если сообщение не найдено
     * @throws NotOwner             Если сообщение не принадлежит сотруднику
     * @throws IllegalFields        Если сообщение пустое или изменений нет
     */
    public void updateMessageFromWeb(Long editMessageId, String text, Employee author) throws TelegramApiException, EntryNotFound, NotOwner, IllegalFields {
        stompController.updateMessage(chatMessageDispatcher.updateMessageFromWeb(editMessageId, text, author, this));
    }

    private void deleteMessageFromTelegram(ChatMessage chatMessage) throws TelegramApiException {
        for (TelegramMessageBind telegramMessageBind : chatMessage.getTelegramBinds()) {
            new TelegramMessageFactory(telegramMessageBind.getTelegramChatId().toString(), mainBot).deleteMessage(telegramMessageBind.getTelegramMessageId()).execute();
        }
    }

    public void sendMessageFromTlgChat(Message receivedMessage) throws EntryNotFound, TelegramApiException, IllegalFields, SaveEntryFailed, IllegalMediaType, ExceptionInsideThread {
        // Получаем автора сообщения
        Employee author = employeeDispatcher.getByTelegramId(receivedMessage.getChatId()).orElseThrow(() -> new EntryNotFound("Идентификатор телеграм не привязан ни к одному аккаунту"));
        // Пытаемся получить активный журнал задачи монтажника который написал сообщение,
        // чтобы понять в какой чат транслировать сообщение.
        WorkLogDto activeWorkLog = workLogDispatcher.getAcceptedByTelegramId(receivedMessage.getChatId());
        // Получаем целевой чат
        ChatDto chat = activeWorkLog.getChat();
        if (chat == null) throw new IllegalFields("Чат не прикреплен к журналу работ");

        switch (Utils.getTlgMsgType(receivedMessage)) {
            case TEXT, MEDIA -> {
                // Создает сообщение в базе данных
                SuperMessage textMessage = chatDispatcher.createMessage(chat.getChatId(), receivedMessage, author, this);
                broadcastUpdatesToWeb(textMessage);
            }
            case GROUP -> {
                String group = Utils.getTlgMsgGroupId(receivedMessage);
                if (groupedMessagesFromTelegram.containsKey(group)) {
                    groupedMessagesFromTelegram.get(group).add(receivedMessage);
                } else {
                    groupedMessagesFromTelegram.put(group, Stream.of(receivedMessage).collect(Collectors.toList()));
                    try {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                // Ждем несколько секунд пока придут все сообщения от telegram api
                                sleep(1000);
                                List<Message> listOfReceivedMessages = groupedMessagesFromTelegram.get(group);
                                SuperMessage mediaGroupMessage = chatDispatcher.createMessage(chat.getChatId(), listOfReceivedMessages, author, this);
                                // Отправляет сообщение для обновления пользовательского интерфейса
                                broadcastUpdatesToWeb(mediaGroupMessage);
                            } catch (InterruptedException | TelegramApiException | EntryNotFound |
                                     IllegalFields | IllegalMediaType e) {
                                throw new RuntimeException(e);
                            } finally {
                                groupedMessagesFromTelegram.remove(group);
                            }
                        });
                    } catch (RuntimeException e) {
                        throw new ExceptionInsideThread(e.getMessage());
                    }
                }
            }
        }
    }

    public void broadcastUpdatesToWeb(SuperMessage message) throws EntryNotFound {
        if (message != null) {
            stompController.createMessage(message);
            ChatDto chat = chatDispatcher.getChatDto(message.getParentChatId());
            stompController.updateChat(ChatMapper.fromDto(chat));
            refreshChatUnreadCount(ChatMapper.fromDto(chat), message);
        }
    }

    /**
     * Обновляет полученное из телеграм сообщение в базе данных
     *
     * @param receivedMessage Сообщение от Telegram Api
     * @throws EntryNotFound        Если сообщение не найдено в базе данных
     * @throws TelegramApiException Если возникла ошибка при отправке обновленного сообщения в Telegram Api
     * @throws IllegalFields        Если нет прикрепленного файла для обновления
     */
    public void updateMessageFromTlgChat(Message receivedMessage) throws EntryNotFound, TelegramApiException, IllegalFields {
        stompController.updateMessage(chatMessageDispatcher.updateMessageFromTlg(receivedMessage, this));
    }

    // TODO Исправить ошибку броадкаста сообщений пользователю который уже закрыл журнал работ

    /**
     * Отправляет сообщения броадкастом во все телеграм чаты
     *
     * @param chat    {@link Chat} из базы данных
     * @param message Отправляемое текстовое сообщение
     * @return Список ответов от telegram api об отправленных сообщениях
     * @throws TelegramApiException Если возникла ошибка при отправке сообщения
     */
    public List<Message> sendTextBroadcastMessage(Chat chat, ChatMessageDto message) throws TelegramApiException {
        // Список ответов от telegram об отправленных сообщениях
        List<Message> responses = new ArrayList<>();

        // Проходимся по всем членам чата, проверяем привязан ли телеграм, и не является ли целевой чат автором сообщения
        for (Employee employee : chat.getMembers()) {
            // Идентификатор чата в телеграм
            String targetChatId = employee.getTelegramUserId();

            // Проверка валидности чата
            if (targetChatId == null || targetChatId.isBlank() || employee.getLogin().equals(message.getAuthor().getLogin()))
                continue;

            responses.add(TelegramMessageFactory.create(targetChatId, mainBot).broadcastTextMessage(message).execute());
        }
        return responses;
    }

    /**
     * Отправляет медиа сообщение с вложением. Автоматически определяет тип вложения и отправляет соответствующий запрос.
     *
     * @param chat    {@link Chat} из базы данных
     * @param message Сообщение для отправки
     * @return Список ответов от telegram api об отправленных сообщениях
     * @throws TelegramApiException Если возникла ошибка при отправке сообщения
     * @throws IllegalFields        Если в сообщении нет вложения
     * @throws IllegalMediaType     Если задан не верный тип данных для вложения
     */
    public List<Message> sendMediaBroadcastMessage(Chat chat, ChatMessageDto message) throws TelegramApiException, IllegalFields, IllegalMediaType {
        // Список ответов от telegram об отправленных сообщениях
        List<Message> responses = new ArrayList<>();

        // Проходимся по всем членам чата, проверяем привязан ли телеграм, и не является ли целевой чат автором сообщения
        for (Employee employee : chat.getMembers()) {
            // Идентификатор чата в телеграм
            String targetChatId = employee.getTelegramUserId();

            // Проверка валидности чата
            if (targetChatId == null || targetChatId.isBlank() || employee.getLogin().equals(message.getAuthor().getLogin()))
                continue;

            responses.add(TelegramMessageFactory.create(targetChatId, mainBot).broadcastMediaMessage(message).execute());
        }
        return responses;
    }

    /**
     * Отправляет медиа группу броадкастом во все телеграм чаты
     *
     * @param chat     Chat из базы данных
     * @param messages Группа отправляемых сообщений, требуется чтобы вложения в сообщениях были либо только визуальные
     *                 (фото, видео), либо аудио, либо документы и файлы. Главное не смешивать
     * @return Список ответов итерированный по списку телеграм чатов
     * @throws TelegramApiException Если возникла ошибка при отправке сообщения
     */
    public List<List<Message>> sendMediaGroupBroadcastMessage(Chat chat, List<ChatMessageDto> messages) throws TelegramApiException {
        if (messages.isEmpty()) return new ArrayList<>();
        List<List<Message>> responses = Stream.generate(ArrayList<Message>::new).limit(messages.size()).collect(Collectors.toList());
        for (Employee employee : chat.getMembers()) {
            String targetChatId = employee.getTelegramUserId();
            if (targetChatId == null || targetChatId.isBlank() || employee.getLogin().equals(messages.get(0).getAuthor().getLogin()))
                continue;
            List<Message> messageList = TelegramMessageFactory.create(targetChatId, mainBot).broadcastMediaGroupMessage(messages).execute();
            for (int i = 0; i < messageList.size(); i++) {
                responses.get(i).add(messageList.get(i));
            }
        }
        return responses;
    }

    public void editTextBroadcastMessage(ChatMessage message, Long sourceChatId) throws TelegramApiException {
        for (TelegramMessageBind bind : message.getTelegramBinds()) {
            if (bind.getTelegramChatId().equals(sourceChatId)) continue;
            TelegramMessageFactory.create(bind.getTelegramChatId(), mainBot).broadcastEditTextMessage(bind.getTelegramMessageId(), message).execute();
        }
    }

    public void editCaptionBroadcastMessage(ChatMessage message, Long sourceChatId) throws TelegramApiException {
        for (TelegramMessageBind bind : message.getTelegramBinds()) {
            if (bind.getTelegramChatId().equals(sourceChatId)) continue;
            TelegramMessageFactory.create(bind.getTelegramChatId(), mainBot).broadcastEditCaptionMessage(bind.getTelegramMessageId(), message).execute();
        }
    }

    public void editMediaBroadcastMessage(ChatMessage messageByBind, Long chatId) throws IllegalFields, TelegramApiException {
        for (TelegramMessageBind bind : messageByBind.getTelegramBinds()) {
            if (bind.getTelegramChatId().equals(chatId)) continue;
            TelegramMessageFactory.create(bind.getTelegramChatId(), mainBot).broadcastEditMediaMessage(bind.getTelegramMessageId(), messageByBind).execute();
        }
    }

    public void assignInstallers(WorkLog workLog, Employee employee) throws TelegramApiException {
        for (Employee installer : workLog.getEmployees()) {
            if (installer.getTelegramUserId() == null || installer.getTelegramUserId().isBlank())
                continue;
            TelegramMessageFactory.create(installer.getTelegramUserId(), mainBot).acceptWorkLog(workLog, employee).execute();
        }
    }
}
