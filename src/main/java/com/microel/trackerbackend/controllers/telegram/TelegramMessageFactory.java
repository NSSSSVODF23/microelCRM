package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.controllers.telegram.handle.Decorator;
import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.dto.chat.TelegramMessageBindDto;
import com.microel.trackerbackend.storage.dto.task.ModelItemDto;
import com.microel.trackerbackend.storage.dto.task.TaskDto;
import com.microel.trackerbackend.storage.dto.task.WorkLogDto;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.exceptions.IllegalMediaType;
import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class TelegramMessageFactory {

    private String chatId;
    private MainBot context;

    public static TelegramMessageFactory create(@NotNull Long chatId, @Nullable MainBot context) {
        if (chatId == null) throw new NullPointerException("Пустой chatId");
        if (context == null) throw new NullPointerException("Telegram бот не инициализирован");
        return new TelegramMessageFactory(String.valueOf(chatId), context);
    }

    public static TelegramMessageFactory create(@NotNull String chatId, @Nullable MainBot context) {
        if (chatId == null) throw new NullPointerException("Пустой chatId");
        if (context == null) throw new NullPointerException("Telegram бот не инициализирован");
        return new TelegramMessageFactory(chatId, context);
    }

    public AbstractExecutor<Message> userIdResponse() {
        return new MessageExecutor<>(
                new SendMessage(chatId, "Ваш TelegramId: " + chatId + "\n Введите его в настройках приложения, чтобы получать сообщения."),
                context
        );
    }

    public AbstractExecutor<Message> acceptWorkLog(WorkLog workLog, Employee employee) {
        String employeeName = employee.getLastName() + " " + employee.getFirstName();
        Task task = workLog.getTask();
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder
                .append("\uD83D\uDC77\u200D♂️ ")
                .append(Decorator.bold("Задача #" + task.getTaskId())).append("\n")
                .append("Тип: ").append(Decorator.bold(task.getModelWireframe().getName())).append("\n")
                .append(Decorator.mention(employeeName, employee.getTelegramUserId()))
                .append(" назначил: ")
                .append(workLog.getEmployees().stream().map(e->Decorator.mention(e.getFullName(), e.getTelegramUserId())).collect(Collectors.joining(", ")))
                .append(" на выполнение задачи");

        InlineKeyboardButton acceptButton = InlineKeyboardButton.builder()
                .text("Принять задачу")
                .callbackData("#accept_work_log:" + workLog.getWorkLogId())
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder.toString())
                .parseMode("HTML")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(acceptButton)).build())
                .build();

        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> task(TaskDto task) {
        List<ModelItemDto> fields = task.getFields();
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Задача #").append(task.getTaskId()).append("\n\n");
        for (ModelItemDto field : fields) {
            messageBuilder.append(Decorator.italic(field.getName())).append(": ").append(field.getTextRepresentationForTlg()).append("\n\n");
        }
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder.toString())
                .parseMode("HTML")
                .build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> offsiteMenu() {
        InlineKeyboardButton active = InlineKeyboardButton.builder()
                .text("Текущая задача")
                .callbackData(CallbackData.create("main_menu", "active_task"))
                .build();
        InlineKeyboardButton queue = InlineKeyboardButton.builder()
                .text("Список задач")
                .callbackData(CallbackData.create("main_menu", "tasks_queue"))
                .build();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие:")
                .replyMarkup(
                        InlineKeyboardMarkup.builder()
                                .keyboardRow(List.of(active, queue))
                                .build()
                )
                .build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> simpleMessage(String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text(text)
                .build();
        return new MessageExecutor<>(message, context);
    }

    @Nullable
    public SendMediaBotMethod<Message> chatMedia(ChatMessageDto chatMessage) {
        if (chatMessage.getAttachment() == null) {
            throw new NullPointerException("Создание сообщения не возможно, так как не задан attachment");
        }
        switch (chatMessage.getAttachment().getType()) {
            case PHOTO:
                return SendPhoto.builder()
                        .chatId(chatId)
                        .caption(chatMessage.getText())
                        .photo(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .build();
            case VIDEO:
                return SendVideo.builder()
                        .chatId(chatId)
                        .caption(chatMessage.getText())
                        .video(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .build();
            case AUDIO:
                return SendAudio.builder()
                        .chatId(chatId)
                        .caption(chatMessage.getText())
                        .audio(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .build();
            case DOCUMENT:
            case FILE:
                return SendDocument.builder()
                        .chatId(chatId)
                        .caption(chatMessage.getText())
                        .document(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .build();
        }
        return null;
    }

    public AbstractExecutor<Message> unknownCommand() {
        SendMessage message = new SendMessage(chatId, "Команда не распознана. Попробуйте еще раз.");
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> workLogListItem(WorkLogDto workLog, Boolean isActive, Boolean isAccepting, @Nullable Integer index) {
        TaskDto task = workLog.getTask();
        StringBuilder messageBuilder = new StringBuilder();
        if (index != null) {
            messageBuilder.append(Decorator.italic("№" + index + " ")).append("\n");
        } else if (isActive) {
            messageBuilder.append(Decorator.bold("✅ Текущая активная")).append("\n");
        }
        messageBuilder.append("Задача #").append(task.getTaskId()).append("\n");
        messageBuilder.append("Тип: ").append(task.getModelWireframe().getName());

        InlineKeyboardButton acceptButton = InlineKeyboardButton.builder()
                .text("Принять задачу")
                .callbackData("#accept_work_log:" + workLog.getWorkLogId())
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder.toString())
                .parseMode("HTML")
                .build();

        if (isAccepting)
            message.setReplyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(acceptButton)).build());

        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> broadcastTextMessage(ChatMessageDto chatMessage) {
        StringBuilder messageBuilder = new StringBuilder("✉️")
                .append(Decorator.mention(chatMessage.getAuthor().getFullName(),chatMessage.getAuthor().getTelegramUserId()));
        if(chatMessage.getText() != null && !chatMessage.getText().isBlank()){
            messageBuilder.append(":\n").append(chatMessage.getText());
        }
        SendMessage message = new SendMessage(chatId, messageBuilder.toString());
        if(chatMessage.getReplyTo() !=null && chatMessage.getReplyTo().getTelegramBinds() != null){
            chatMessage.getReplyTo().getTelegramBinds().stream().filter(b->b.getTelegramChatId().toString().equals(chatId)).findFirst().ifPresent(bind->
                    message.setReplyToMessageId(bind.getTelegramMessageId())
            );
        }
        message.setParseMode(ParseMode.HTML);
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Serializable> broadcastEditTextMessage(Integer telegramMessageId, ChatMessage chatMessage) {
        StringBuilder messageBuilder = new StringBuilder("✉️")
                .append(Decorator.mention(chatMessage.getAuthor().getFullName(), chatMessage.getAuthor().getTelegramUserId()));
        messageBuilder.append(":\n").append(chatMessage.getText());
        return new MessageExecutor<>(EditMessageText.builder()
                .chatId(chatId)
                .messageId(telegramMessageId)
                .text(messageBuilder.toString())
                .parseMode("HTML")
                .build(), context);
    }

    public AbstractExecutor<Serializable> broadcastEditCaptionMessage(Integer telegramMessageId, ChatMessage message) {
        StringBuilder messageBuilder = new StringBuilder("✉️")
                .append(Decorator.mention(message.getAuthor().getFullName(), message.getAuthor().getTelegramUserId()));
        messageBuilder.append(":\n").append(message.getText());
        return new MessageExecutor<>(EditMessageCaption.builder()
                .chatId(chatId)
                .messageId(telegramMessageId)
                .caption(messageBuilder.toString())
                .parseMode("HTML")
                .build(), context);
    }

    public AbstractExecutor<Serializable> broadcastEditMediaMessage(Integer telegramMessageId, ChatMessage messageByBind) throws IllegalFields {

        InputMedia inputMedia = Attachment.getInputMedia(messageByBind.getAttachment());
        if(inputMedia == null)
            throw new IllegalFields("Нет прикрепленного файла для обновления");

        EditMessageMedia editMessageMedia = EditMessageMedia.builder()
                .chatId(chatId)
                .messageId(telegramMessageId)
                .media(inputMedia)
                .build();

        return new EditMediaMessageExecutor(editMessageMedia, context);
    }

    public AbstractExecutor<List<Message>> broadcastMediaGroupMessage(List<ChatMessageDto> chatMessages) {
        List<InputMedia> mediaList = chatMessages.stream().map(m-> Attachment.getInputMedia(m.getAttachment())).filter(Objects::nonNull).collect(Collectors.toList());
        SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId, mediaList);
        if(chatMessages.get(0).getReplyTo() !=null && chatMessages.get(0).getReplyTo().getTelegramBinds() != null){
            chatMessages.get(0).getReplyTo().getTelegramBinds().stream().filter(b->b.getTelegramChatId().toString().equals(chatId)).findFirst().ifPresent(bind->
                    sendMediaGroup.setReplyToMessageId(bind.getTelegramMessageId())
            );
        }
        return new GroupMessageExecutor(sendMediaGroup, context);
    }

    /**
     * Создает мультимедиа-сообщение с вложением для рассылки
     * @param chatMessage Объект сообщения с вложением
     * @return {@link AbstractExecutor} c мультимедиа-сообщением
     * @throws IllegalFields Если в сообщении нет вложения
     * @throws IllegalMediaType Если во вложении задан не верный тип данных
     */
    public AbstractExecutor<Message> broadcastMediaMessage(ChatMessageDto chatMessage) throws IllegalFields, IllegalMediaType {
        StringBuilder messageBuilder = new StringBuilder("✉️")
                .append(Decorator.mention(chatMessage.getAuthor().getFullName(),chatMessage.getAuthor().getTelegramUserId()));
        if(chatMessage.getText() != null && !chatMessage.getText().isBlank()){
            messageBuilder.append(":\n").append(chatMessage.getText());
        }

        Integer replyMessageId = null;
        if(chatMessage.getReplyTo() !=null && chatMessage.getReplyTo().getTelegramBinds() != null){
            replyMessageId = chatMessage.getReplyTo().getTelegramBinds().stream()
                    .filter(b->b.getTelegramChatId().toString().equals(chatId))
                    .map(TelegramMessageBindDto::getTelegramMessageId)
                    .findFirst().orElse(null);
        }

        if(chatMessage.getAttachment() == null){
            throw new IllegalFields("Мультимедиа сообщение не создано так как нет вложения");
        }

        switch (chatMessage.getAttachment().getType()) {
            case PHOTO:
                return new PhotoMessageExecutor(SendPhoto.builder()
                        .chatId(chatId)
                        .caption(messageBuilder.toString())
                        .photo(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .replyToMessageId(replyMessageId)
                        .build(), context);
            case VIDEO:
                return new VideoMessageExecutor(SendVideo.builder()
                        .chatId(chatId)
                        .caption(messageBuilder.toString())
                        .video(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .replyToMessageId(replyMessageId)
                        .build(), context);
            case AUDIO:
                return new AudioMessageExecutor(SendAudio.builder()
                        .chatId(chatId)
                        .caption(messageBuilder.toString())
                        .audio(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .replyToMessageId(replyMessageId)
                        .build(), context);
            case DOCUMENT:
            case FILE:
                return new DocumentMessageExecutor(SendDocument.builder()
                        .chatId(chatId)
                        .caption(messageBuilder.toString())
                        .document(chatMessage.getAttachment().getInputFile())
                        .parseMode("HTML")
                        .replyToMessageId(replyMessageId)
                        .build(), context);
            default:
                throw new IllegalMediaType("Не известный тип медиа вложения");
        }
    }

    public AbstractExecutor<Boolean> deleteMessage(Integer telegramMessageId) {
        return new MessageExecutor<>(DeleteMessage.builder().chatId(chatId).messageId(telegramMessageId).build(), context);
    }

    /**
     * Позволяет отправить запрос в Telegram API и получить результат согласно заданному контексту
     * @param <T> Возвращаемое значение от Telegram API
     */
    public interface AbstractExecutor<T>{
        T execute() throws TelegramApiException;
    }

    public static class MessageExecutor<T extends Serializable, Method extends BotApiMethod<T>> implements AbstractExecutor<T> {
        private final Method message;
        private final MainBot context;

        public MessageExecutor(Method message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public T execute() throws TelegramApiException {
            return context.execute(message);
        }
    }

    public static class EditMediaMessageExecutor implements AbstractExecutor<Serializable> {
        private final EditMessageMedia message;
        private final MainBot context;

        public EditMediaMessageExecutor(EditMessageMedia message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Serializable execute() throws TelegramApiException {
            return context.execute(message);
        }
    }

    public static class PhotoMessageExecutor implements AbstractExecutor<Message> {
        private final SendPhoto message;
        private final MainBot context;

        public PhotoMessageExecutor(SendPhoto message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Message execute() throws TelegramApiException {
            return context.execute(message);
        }
    }

    public static class VideoMessageExecutor implements AbstractExecutor<Message> {
        private final SendVideo message;
        private final MainBot context;

        public VideoMessageExecutor(SendVideo message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Message execute() throws TelegramApiException {
            return context.execute(message);
        }
    }

    public static class AudioMessageExecutor implements AbstractExecutor<Message> {
        private final SendAudio message;
        private final MainBot context;

        public AudioMessageExecutor(SendAudio message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Message execute() throws TelegramApiException {
            return context.execute(message);
        }
    }

    public static class EditTextMessageExecutor implements AbstractExecutor<Serializable> {
        private final EditMessageText message;
        private final MainBot context;

        public EditTextMessageExecutor(EditMessageText message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Serializable execute() throws TelegramApiException {
            return context.execute(message);
        }
    }

    public static class DocumentMessageExecutor implements AbstractExecutor<Message> {
        private final SendDocument message;
        private final MainBot context;

        public DocumentMessageExecutor(SendDocument message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Message execute() throws TelegramApiException {
            return context.execute(message);
        }
    }

    public static class GroupMessageExecutor implements AbstractExecutor<List<Message>> {
        private final SendMediaGroup message;
        private final MainBot context;

        public GroupMessageExecutor(SendMediaGroup message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public List<Message> execute() throws TelegramApiException {
            return context.execute(message);
        }
    }
}
