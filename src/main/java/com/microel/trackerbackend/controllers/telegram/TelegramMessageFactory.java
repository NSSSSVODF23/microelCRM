package com.microel.trackerbackend.controllers.telegram;

import com.microel.tdo.pon.OpticalLineTerminal;
import com.microel.tdo.pon.events.OntStatusChangeEvent;
import com.microel.trackerbackend.controllers.telegram.handle.Decorator;
import com.microel.trackerbackend.misc.DhcpIpRequestNotificationBody;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.external.RestPage;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.SwitchBaseInfo;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.dto.chat.TelegramMessageBindDto;
import com.microel.trackerbackend.storage.entities.acp.NetworkConnectionLocation;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.chat.ChatMessage;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import com.microel.trackerbackend.storage.entities.filesys.TFile;
import com.microel.trackerbackend.storage.entities.tariff.AutoTariff;
import com.microel.trackerbackend.storage.entities.task.Contract;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.WorkLogTargetFile;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.TelegramOptions;
import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.entities.userstlg.UserTariff;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.exceptions.IllegalMediaType;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Ваш TelegramId: " + Decorator.code(chatId) + "\n Введите его в настройках приложения, чтобы получать сообщения.")
                .parseMode("HTML")
                .build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> groupIdResponse() {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Id группового чата: " + Decorator.code(chatId) + "\n Назначьте его всем монтажникам из данной группы в настройках сотрудников.")
                .parseMode("HTML")
                .build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> acceptWorkLog(WorkLog workLog, Employee employee) {
        String employeeName = employee.getLastName() + " " + employee.getFirstName();
        Task task = workLog.getTask();
        String messageBuilder = "\uD83D\uDC77\u200D♂️ " +
                Decorator.bold("Задача #" + task.getTaskId()) + "\n" +
                Decorator.bold(task.getModelWireframe().getName()) + " - " + Decorator.bold(task.getCurrentStage().getLabel()) + "\n" +
                Decorator.mention(employeeName, employee.getTelegramUserId()) +
                " назначил: " +
                workLog.getEmployees().stream().map(e -> Decorator.mention(e.getFullName(), e.getTelegramUserId())).collect(Collectors.joining(", ")) +
                " на выполнение задачи";

        InlineKeyboardButton acceptButton = InlineKeyboardButton.builder()
                .text("Принять задачу")
                .callbackData("#accept_work_log:" + workLog.getWorkLogId())
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder)
                .parseMode("HTML")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(acceptButton)).build())
                .build();

        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> infoModeCancel(String info, String cancelCallback) {
        InlineKeyboardButton cancelButton = InlineKeyboardButton.builder()
                .text("Отменить")
                .callbackData("#cancel_mode:" + cancelCallback)
                .build();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text(info)
                .replyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(cancelButton)).build())
                .build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> currentActiveTask(Task task) {
        List<ModelItem> fields = task.getFields().stream().filter(ModelItem::nonEmpty).filter(ModelItem::isDisplayToTelegram).toList();

        KeyboardRow keyboardRowMenu = new KeyboardRow(List.of(new KeyboardButton("ℹ️ Меню задачи")));
        KeyboardRow keyboardRowClose = new KeyboardRow(List.of(new KeyboardButton("\uD83D\uDC4C Завершить задачу")));
        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder().keyboard(List.of(keyboardRowMenu, keyboardRowClose)).resizeKeyboard(true).build();
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(Decorator.bold(task.getClassName()));
        if (!task.getTypeName().equals("Неизвестно")) {
            messageBuilder.append(" - ").append(Decorator.bold(task.getTypeName()));
        }
        messageBuilder.append(" #").append(task.getTaskId()).append("\n");
        for (ModelItem field : fields) {
            messageBuilder.append(Decorator.underline(field.getName())).append(": ").append(field.getTextRepresentationForTlg()).append("\n");
        }
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder.toString())
                .parseMode("HTML")
                .replyMarkup(keyboardMarkup)
                .build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> requiringDeliveryOfAContract(WorkLog workLog) {
        StringBuilder messageBuilder = new StringBuilder();

        Task task = workLog.getTask();
        List<ModelItem> fields = task.getFields().stream().filter(ModelItem::nonEmpty).filter(ModelItem::isDisplayToTelegram).toList();

        messageBuilder.append(" #").append(task.getTaskId()).append("\n");
        for (ModelItem field : fields) {
            messageBuilder.append(Decorator.underline(field.getName())).append(": ").append(field.getTextRepresentationForTlg()).append("\n");
        }

        messageBuilder.append("\n\nНужные договора:\n");

        List<String> contractList = workLog.getConcludedContracts().stream().filter(Contract::isUnreceived).map(Contract::toTelegramString).toList();

        for (String contract : contractList) {
            messageBuilder.append(Decorator.bold(contract)).append("\n");
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder.toString())
                .parseMode("HTML")
                .build();

        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> currentActiveTaskForGroupChat(Task task) {
        List<ModelItem> fields = task.getFields().stream().filter(ModelItem::nonEmpty).filter(ModelItem::isDisplayToTelegram).toList();
        ReplyKeyboardRemove clearKeyboardMarkup = ReplyKeyboardRemove.builder().removeKeyboard(true).build();

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(Decorator.bold(task.getClassName()));
        if (!task.getTypeName().equals("Неизвестно")) {
            messageBuilder.append(" - ").append(Decorator.bold(task.getTypeName()));
        }
        messageBuilder.append(" #").append(task.getTaskId()).append("\n");
        for (ModelItem field : fields) {
            messageBuilder.append(Decorator.underline(field.getName())).append(": ").append(field.getTextRepresentationForTlg()).append("\n");
        }
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(messageBuilder.toString())
                .replyMarkup(clearKeyboardMarkup)
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

    public CallbackAnswerExecutor answerCallback(String cbQueryId, @Nullable String text) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder().text(text).callbackQueryId(cbQueryId).showAlert(false).build();
        return new CallbackAnswerExecutor(answer, context);
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

    public AbstractExecutor<Message> workLogListItem(WorkLog workLog, Boolean isActive, Boolean isAccepting, @Nullable Integer index) {
        Task task = workLog.getTask();
        StringBuilder messageBuilder = new StringBuilder();
        if (index != null) {
            messageBuilder.append(Decorator.italic("№" + index + " ")).append("\n");
        } else if (isActive) {
            messageBuilder.append(Decorator.bold("✅ Текущая активная")).append("\n");
        }

        messageBuilder.append(Decorator.bold(task.getClassName()));
        if (!task.getTypeName().equals("Неизвестно")) {
            messageBuilder.append(" - ").append(Decorator.bold(task.getTypeName()));
        }
        messageBuilder.append(" #").append(task.getTaskId()).append("\n");

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
                .append(Decorator.mention(chatMessage.getAuthor().getFullName(), chatMessage.getAuthor().getTelegramUserId()));
        if (chatMessage.getText() != null && !chatMessage.getText().isBlank()) {
            messageBuilder.append(":\n").append(chatMessage.getText());
        }
        SendMessage message = new SendMessage(chatId, messageBuilder.toString());
        if (chatMessage.getReplyTo() != null && chatMessage.getReplyTo().getTelegramBinds() != null) {
            chatMessage.getReplyTo().getTelegramBinds().stream().filter(b -> b.getTelegramChatId().toString().equals(chatId)).findFirst().ifPresent(bind ->
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
        if (inputMedia == null)
            throw new IllegalFields("Нет прикрепленного файла для обновления");

        EditMessageMedia editMessageMedia = EditMessageMedia.builder()
                .chatId(chatId)
                .messageId(telegramMessageId)
                .media(inputMedia)
                .build();

        return new EditMediaMessageExecutor(editMessageMedia, context);
    }

    public AbstractExecutor<Message> broadcastMediaGroupMessage(List<ChatMessageDto> chatMessages) {
        List<InputMedia> mediaList = chatMessages.stream().map(m -> Attachment.getInputMedia(m.getAttachment())).filter(Objects::nonNull).collect(Collectors.toList());
        SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId, mediaList);
        if (chatMessages.get(0).getReplyTo() != null && chatMessages.get(0).getReplyTo().getTelegramBinds() != null) {
            chatMessages.get(0).getReplyTo().getTelegramBinds().stream().filter(b -> b.getTelegramChatId().toString().equals(chatId)).findFirst().ifPresent(bind ->
                    sendMediaGroup.setReplyToMessageId(bind.getTelegramMessageId())
            );
        }
        return new GroupMessageExecutor(sendMediaGroup, context);
    }

    /**
     * Создает мультимедиа-сообщение с вложением для рассылки
     *
     * @param chatMessage Объект сообщения с вложением
     * @return {@link AbstractExecutor} c мультимедиа-сообщением
     * @throws IllegalFields    Если в сообщении нет вложения
     * @throws IllegalMediaType Если во вложении задан не верный тип данных
     */
    public AbstractExecutor<Message> broadcastMediaMessage(ChatMessageDto chatMessage) throws IllegalFields, IllegalMediaType {
        StringBuilder messageBuilder = new StringBuilder("✉️")
                .append(Decorator.mention(chatMessage.getAuthor().getFullName(), chatMessage.getAuthor().getTelegramUserId()));
        if (chatMessage.getText() != null && !chatMessage.getText().isBlank()) {
            messageBuilder.append(":\n").append(chatMessage.getText());
        }

        Integer replyMessageId = null;
        if (chatMessage.getReplyTo() != null && chatMessage.getReplyTo().getTelegramBinds() != null) {
            replyMessageId = chatMessage.getReplyTo().getTelegramBinds().stream()
                    .filter(b -> b.getTelegramChatId().toString().equals(chatId))
                    .map(TelegramMessageBindDto::getTelegramMessageId)
                    .findFirst().orElse(null);
        }

        if (chatMessage.getAttachment() == null) {
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

    public AbstractExecutor<Message> clearKeyboardMenu() {
        ReplyKeyboardRemove clearKeyboardMarkup = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
        return new MessageExecutor<>(SendMessage.builder().chatId(chatId).text("Меню очищено").replyMarkup(clearKeyboardMarkup).build(), context);
    }

    public AbstractExecutor<Message> closeWorkLogMessage() {
        List<InlineKeyboardButton> inlineKeyboardButtons = List.of(
                InlineKeyboardButton.builder().text("\uD83D\uDCC4 Отправить отчет").callbackData("#send_report").build(),
                InlineKeyboardButton.builder().text("\uD83D\uDE45\u200D♂️ Отменить закрытие").callbackData("#cancel_close").build()
        );
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboardRow(inlineKeyboardButtons).build();
        return new MessageExecutor<>(SendMessage.builder()
                .chatId(chatId)
                .text("Вы находитесь в режиме закрытия задачи, напишите отчет (одно или несколько сообщений) и нажмите кнопку \"Отправить отчет\"")
                .replyMarkup(inlineKeyboardMarkup)
                .build(),
                context
        );
    }

    public AbstractExecutor<Message> dhcpIpRequestNotification(DhcpIpRequestNotificationBody body) {
        String messageBuilder = "\uD83D\uDCF6️" +
                Decorator.bold("Обнаружено подключенное оборудование:") + "\n" +
                "Хозяин: " + body.getOwner() + "\n" +
                "Device: " + body.getDevice() + "\n" +
                "IP: " + body.getIp() + "\n" +
                "MAC: " + body.getMac() + "\n" +
                "VLAN: " + body.getVlan() + "\n";

//        if(body.getSwitches() != null){
//            messageBuilder.append("\n").append(Decorator.bold( "Список коммутаторов:")).append("\n");
//            for(DhcpIpRequestNotificationBody.SwitchInfo switchInfo : body.getSwitches()) {
//                messageBuilder
//                        .append("<pre>")
//                        .append(switchInfo.getName()).append(" ")
//                        .append(switchInfo.getIpaddress()).append(" ")
//                        .append(switchInfo.getModel()).append(" ")
//                        .append(switchInfo.getStreet()).append("-").append(switchInfo.getHouse())
//                        .append("</pre>")
//                        .append("\n");
//            }
//        }

        SendMessage message = new SendMessage(chatId, messageBuilder);
        message.setParseMode(ParseMode.HTML);
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> addressSuggestions(List<House> houseList, String callbackPrefix) {
        List<List<InlineKeyboardButton>> buttons = houseList.stream().map(house -> {
            return List.of(InlineKeyboardButton.builder()
                    .text(house.getAddressName())
                    .callbackData(CallbackData.create(callbackPrefix, house.getHouseId().toString()))
                    .build());
        }).toList();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите адрес:")
                .replyMarkup(
                        InlineKeyboardMarkup.builder()
                                .keyboard(buttons)
                                .build()
                )
                .build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> sessionPage(RestPage<DhcpBinding> lastBindings, Integer buildingId) {

        InlineKeyboardButton load_page = InlineKeyboardButton.builder()
                .text("Ещё...")
                .callbackData(CallbackData.create("load_page", "sessionHouse:" + buildingId + ":" + (lastBindings.getNumber() + 1)))
                .build();

        SendMessage.SendMessageBuilder message = SendMessage.builder()
                .chatId(chatId).parseMode(ParseMode.HTML)
                .text(lastBindings.stream().map(DhcpBinding::getTextRow).collect(Collectors.joining("\n\n")));

        if (lastBindings.isEmpty()) {
            message.text("Данных по сессиям нет");
        }

        if (lastBindings.getNumber() < lastBindings.getTotalPages() - 1) {
            message.replyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(load_page)).build());
        }

        return new MessageExecutor<>(message.build(), context);
    }

    public AbstractExecutor<Message> sessionPage(List<DhcpBinding> bindings) {

        SendMessage.SendMessageBuilder message = SendMessage.builder()
                .chatId(chatId).parseMode(ParseMode.HTML)
                .text(bindings.stream().map(DhcpBinding::getTextRow).collect(Collectors.joining("\n\n")));

        if (bindings.isEmpty()) {
            message.text("Данных по сессиям нет");
        }

        return new MessageExecutor<>(message.build(), context);
    }

    public AbstractExecutor<Message> sessionCommutatorPage(RestPage<DhcpBinding> lastBindings, Integer commutatorId) {

        InlineKeyboardButton load_page = InlineKeyboardButton.builder()
                .text("Ещё...")
                .callbackData(CallbackData.create("load_page", "sessionHouseCommutator:" + commutatorId + ":" + (lastBindings.getNumber() + 1)))
                .build();

        SendMessage.SendMessageBuilder message = SendMessage.builder()
                .chatId(chatId).parseMode(ParseMode.HTML)
                .text(lastBindings.stream().sorted((o1, o2) -> {
                    Integer port1 = null;
                    NetworkConnectionLocation location = o1.getLastConnectionLocation();
                    if (location != null) {
                        try {
                            port1 = Integer.parseInt(location.getPortName());
                        } catch (Exception ignore) {
                        }
                    }
                    Integer port2 = null;
                    location = o2.getLastConnectionLocation();
                    if (location != null) {
                        try {
                            port2 = Integer.parseInt(location.getPortName());
                        } catch (Exception ignore) {
                        }
                    }
                    return Comparator.nullsLast(Integer::compareTo).compare(port1, port2);
                }).map(DhcpBinding::getTextRowWithOnline).collect(Collectors.joining("\n\n")));

        if (lastBindings.isEmpty()) {
            message.text("Данных по сессиям нет");
        }

        if (lastBindings.getNumber() < lastBindings.getTotalPages() - 1) {
            message.replyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(load_page)).build());
        }

        return new MessageExecutor<>(message.build(), context);
    }

    public AbstractExecutor<Message> commutatorSessions(Page<SwitchBaseInfo> commutators) {
        List<List<InlineKeyboardButton>> buttons = commutators.stream().map(com -> {
            return List.of(InlineKeyboardButton.builder()
                    .text(com.getName())
                    .callbackData(CallbackData.create("commutator_sessions_com_sel", com.getId().toString()))
                    .build());
        }).toList();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите коммутатор:")
                .replyMarkup(
                        InlineKeyboardMarkup.builder()
                                .keyboard(buttons)
                                .build()
                ).build();
        return new MessageExecutor<>(message, context);
    }

    public AbstractExecutor<Message> loginInfoMenu(List<ModelItem> fields) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<ModelItem> loginsItems = fields.stream().filter(f -> f.getWireframeFieldType().equals(WireframeFieldType.LOGIN))
                .filter(f -> f.getStringData() != null && !f.getStringData().isBlank()).toList();
        List<ModelItem> addressItems = fields.stream().filter(f -> f.getWireframeFieldType().equals(WireframeFieldType.ADDRESS))
                .filter(f -> f.getAddressData() != null && f.getAddressData().getHouseId() != null).toList();


        List<InlineKeyboardButton> getBillingInfoButtons = loginsItems.stream().map(f -> InlineKeyboardButton.builder()
                .text("Информация по логину " + f.getStringData())
                .callbackData(CallbackData.create("get_billing_info", f.getStringData()))
                .build()).toList();

        List<InlineKeyboardButton> userHardwareButtons = loginsItems.stream().map(f -> InlineKeyboardButton.builder()
                .text("Оборудование абонента " + f.getStringData())
                .callbackData(CallbackData.create("get_user_hardware", f.getStringData()))
                .build()).toList();

//        List<InlineKeyboardButton> authButtons = loginsItems.stream().map(f -> InlineKeyboardButton.builder()
//                .text("Авторизовать логин " + f.getStringData())
//                .callbackData(CallbackData.create("get_auth_variants", f.getStringData()))
//                .build()).toList();

        List<InlineKeyboardButton> aliveCountButtons = addressItems.stream().map(f -> InlineKeyboardButton.builder()
                .text("Живые в " + f.getAddressData().getHouseName())
                .callbackData(CallbackData.create("check_alive_address", f.getAddressData().getHouseId().toString()))
                .build()).toList();

        List<InlineKeyboardButton> hardwareInHouseButtons = addressItems.stream().map(f -> InlineKeyboardButton.builder()
                .text("Оборудование в " + f.getAddressData().getHouseName())
                .callbackData(CallbackData.create("house_sessions_address", f.getAddressData().getHouseId().toString()))
                .build()).toList();

        keyboard.add(getBillingInfoButtons);
        keyboard.add(userHardwareButtons);
//        keyboard.add(authButtons);
        keyboard.add(aliveCountButtons);
        keyboard.add(hardwareInHouseButtons);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите пункт меню:")
                .replyMarkup(
                        InlineKeyboardMarkup.builder()
                                .keyboard(keyboard)
                                .build()
                ).build();
        return new MessageExecutor<>(message, context);

    }

    public AbstractExecutor<Message> billingInfo(ApiBillingController.TotalUserInfo userInfo) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(Decorator.bold("Пользователь")).append("\n\n");
        stringBuilder.append(Decorator.underline("Адрес:")).append(" ").append(userInfo.getIbase().getAddr()).append("\n");
        stringBuilder.append(Decorator.underline("ФИО:")).append(" ").append(userInfo.getIbase().getFio()).append("\n");
        stringBuilder.append(Decorator.underline("Телефон:")).append(" ").append(userInfo.getIbase().getPhone()).append("\n");

        stringBuilder.append("\n\n").append(Decorator.bold("Тарифы")).append("\n\n");

        ApiBillingController.OldTarifItem mainTariff = userInfo.getOldTarif().get(0);
        if (mainTariff != null) {
            stringBuilder.append(Decorator.underline("Основной:")).append("\n");
            stringBuilder.append("   ").append(Decorator.italic(mainTariff.getService())).append(" ").append(mainTariff.getPrice()).append("руб/период").append("\n");
        } else {
            stringBuilder.append(Decorator.underline("Основной:")).append(" ").append(Decorator.italic("Нет тарифа")).append("\n");
        }

        if (userInfo.getOldTarif().size() > 1) {
            stringBuilder.append(Decorator.underline("Сервисы:\n"));
            for (int i = 1; i < userInfo.getOldTarif().size(); i++) {
                ApiBillingController.OldTarifItem service = userInfo.getOldTarif().get(i);
                stringBuilder.append("   ").append(service.getService()).append(" ").append(service.getPrice()).append("руб/период").append("\n");
            }
        }

        if (userInfo.getOldTarif() != null && userInfo.getOldTarif().size() > 0) {
            Float totalPrice = userInfo.getOldTarif().stream().map(ApiBillingController.OldTarifItem::getPrice).reduce(0f, Float::sum);
            stringBuilder.append(Decorator.underline("Общая стоимость:")).append(" ").append(totalPrice).append("руб/период").append("\n");
        }

        stringBuilder.append(Decorator.underline("Скорость:\n"))
                .append("   По тарифу ").append(userInfo.getNewTarif().getTspeed()).append(" Мбит/с").append("\n");
        stringBuilder.append("   Фактическая ").append(userInfo.getNewTarif().getSpeed()).append(" Мбит/с").append("\n");

        if (userInfo.getNewTarif().getEdate() != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            stringBuilder.append(Decorator.underline("Окончание тарифа:")).append(" ").append(format.format(userInfo.getNewTarif().getEdate())).append("\n\n");
        }

        stringBuilder.append(Decorator.bold("Баланс: "));
        Float balance = userInfo.getIbase().getMoney();
        if (balance != null) {
            stringBuilder.append(balance).append(" руб").append("\n");
        } else {
            stringBuilder.append(Decorator.italic("0")).append("\n");
        }

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(stringBuilder.toString())
                .parseMode(ParseMode.HTML)
                .build();

        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> authVariants(String login) {
        InlineKeyboardButton recentlyButton = InlineKeyboardButton.builder()
                .text("Недавно вышедшее в сеть")
                .callbackData(CallbackData.create("auth_recently", login))
                .build();
        InlineKeyboardButton byMacButton = InlineKeyboardButton.builder()
                .text("По мак адресу")
                .callbackData(CallbackData.create("auth_by_mac", login))
                .build();
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите вариант авторизации:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboard(List.of(
                                        List.of(recentlyButton),
                                        List.of(byMacButton)
                                ))
                                .build()
                ).build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> authButtonList(DhcpBinding binding, String login) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text("Авторизовать")
                .callbackData(CallbackData.create("auth_login", login + "#" + binding.getMacaddr()))
                .build();

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(binding.getTextRow())
                .parseMode(ParseMode.HTML)
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboardRow(List.of(button))
                                .build()
                ).build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> fileSuggestions(List<TFile.FileSuggestion> files) {
        List<List<InlineKeyboardButton>> keyboard = files.stream().map(TFile.FileSuggestion::toTelegramButton).map(List::of).toList();
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите нужный файл:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboard(keyboard)
                                .build()
                ).build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> file(TFile file) {
        return switch (file.getType()) {
            case PHOTO -> new PhotoMessageExecutor(SendPhoto.builder()
                    .chatId(chatId)
                    .caption(file.getName())
                    .photo(file.getInputFile())
                    .parseMode("HTML")
                    .build(), context);
            case VIDEO -> new VideoMessageExecutor(SendVideo.builder()
                    .chatId(chatId)
                    .caption(file.getName())
                    .video(file.getInputFile())
                    .parseMode("HTML")
                    .build(), context);
            case AUDIO -> new AudioMessageExecutor(SendAudio.builder()
                    .chatId(chatId)
                    .caption(file.getName())
                    .audio(file.getInputFile())
                    .parseMode("HTML")
                    .build(), context);
            case DOCUMENT, FILE -> new DocumentMessageExecutor(SendDocument.builder()
                    .chatId(chatId)
                    .caption(file.getName())
                    .document(file.getInputFile())
                    .parseMode("HTML")
                    .build(), context);
            default -> throw new IllegalMediaType("Не известный тип медиа вложения");
        };
    }

    public AbstractExecutor<Message> workTargetMessage(@Nullable String message, @Nullable WorkLogTargetFile file) {
        if (message == null || message.isBlank()) {
            message = "Текущая цель";
        } else {
            message = "Текущая цель:\n" + message;
        }
        if (file != null) {
            switch (file.getType()) {
                case PHOTO -> {
                    return new PhotoMessageExecutor(SendPhoto.builder()
                            .chatId(chatId)
                            .caption(message)
                            .photo(file.getInputFile())
                            .parseMode("HTML")
                            .build(), context);
                }
                case VIDEO -> {
                    return new VideoMessageExecutor(SendVideo.builder()
                            .chatId(chatId)
                            .caption(message)
                            .video(file.getInputFile())
                            .parseMode("HTML")
                            .build(), context);
                }
                case AUDIO -> {
                    return new AudioMessageExecutor(SendAudio.builder()
                            .chatId(chatId)
                            .caption(message)
                            .audio(file.getInputFile())
                            .parseMode("HTML")
                            .build(), context);
                }
                case DOCUMENT, FILE -> {
                    return new DocumentMessageExecutor(SendDocument.builder()
                            .chatId(chatId)
                            .caption(message)
                            .document(file.getInputFile())
                            .parseMode("HTML")
                            .build(), context);
                }
                default -> throw new IllegalMediaType("Не известный тип медиа вложения");
            }
        } else {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .parseMode("HTML")
                    .build();
            return new MessageExecutor<>(sendMessage, context);
        }
    }

    public AbstractExecutor<Message> workTargetGroupMessage(@Nullable String message, @Nullable List<WorkLogTargetFile> files) {
        if (files == null || files.isEmpty())
            throw new ResponseException("Нет файлов для отправки");
        if (message == null || message.isBlank()) {
            message = "Текущая цель";
        } else {
            message = "Текущая цель:\n" + message;
        }
        List<InputMedia> mediaList = files.stream().map(WorkLogTargetFile::toInputMedia).filter(Objects::nonNull).collect(Collectors.toList());
        mediaList.get(0).setCaption(message);
        SendMediaGroup sendMediaGroup = new SendMediaGroup(chatId, mediaList);
        return new GroupMessageExecutor(sendMediaGroup, context);
    }

    public AbstractExecutor<Message> workComments(List<Comment> comments, List<Attachment> attachments) {
        StringBuilder sb = new StringBuilder();
        sb.append("Комментарии:\n");
        for (Comment comment : comments) {
            sb.append(comment.getCreator().getFullName()).append(": ").append(Decorator.bold(comment.getSimpleText())).append("\n");
        }
        if (attachments.isEmpty()) {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(sb.toString())
                    .parseMode("HTML")
                    .build();
            return new MessageExecutor<>(sendMessage, context);
        } else {
            if (attachments.size() > 1) {
                try {
                    Map<String, List<Attachment>> atcByTypes = attachments.stream().collect(Collectors.groupingBy(attachment -> {
                        FileType type = attachment.getType();
                        if (type == FileType.PHOTO || type == FileType.VIDEO) return "visual";
                        if (type == FileType.AUDIO) return "audio";
                        if (type == FileType.FILE || type == FileType.DOCUMENT) return "files";
                        return "other";
                    }));
                    List<PartialBotApiMethod<?>> messages = new ArrayList<>();
                    AtomicBoolean isMessageAdded = new AtomicBoolean(false);
                    atcByTypes.forEach((k, v) -> {
                        if (v.size() > 1 && v.size() <= 10) {
                            List<InputMedia> inputMedia = v.stream().map(Attachment::getInputMedia).filter(Objects::nonNull).toList();
                            if (!isMessageAdded.get()) {
                                isMessageAdded.set(true);
                                inputMedia.get(0).setCaption(sb.toString());
                            }
                            messages.add(new SendMediaGroup(chatId, inputMedia));
                        } else if (v.size() == 1) {
                            SendMediaBotMethod<Message> mediaMessage = null;
                            switch (v.get(0).getType()) {
                                case PHOTO:
                                    mediaMessage = SendPhoto.builder()
                                            .chatId(chatId)
                                            .caption(!isMessageAdded.get() ? sb.toString() : null)
                                            .photo(v.get(0).getInputFile())
                                            .parseMode("HTML")
                                            .build();

                                case VIDEO:
                                    mediaMessage = SendVideo.builder()
                                            .chatId(chatId)
                                            .caption(!isMessageAdded.get() ? sb.toString() : null)
                                            .video(v.get(0).getInputFile())
                                            .parseMode("HTML")
                                            .build();
                                case AUDIO:
                                    mediaMessage = SendAudio.builder()
                                            .chatId(chatId)
                                            .caption(!isMessageAdded.get() ? sb.toString() : null)
                                            .audio(v.get(0).getInputFile())
                                            .parseMode("HTML")
                                            .build();
                                case DOCUMENT:
                                case FILE:
                                    mediaMessage = SendDocument.builder()
                                            .chatId(chatId)
                                            .caption(!isMessageAdded.get() ? sb.toString() : null)
                                            .document(v.get(0).getInputFile())
                                            .parseMode("HTML")
                                            .build();
                            }
                            if (mediaMessage != null) {
                                if (!isMessageAdded.get()) {
                                    isMessageAdded.set(true);
                                }
                                messages.add(mediaMessage);
                            }
                        }
                    });
                    return new MultiMessageExecutor(messages, context);
                } catch (Exception e) {
                    SendMessage sendMessage = SendMessage.builder()
                            .chatId(chatId)
                            .text(sb.toString())
                            .parseMode("HTML")
                            .build();
                    return new MessageExecutor<>(sendMessage, context);
                }
            } else {
                Attachment attachment = attachments.get(0);
                switch (attachment.getType()) {
                    case PHOTO:
                        return new PhotoMessageExecutor(SendPhoto.builder()
                                .chatId(chatId)
                                .caption(sb.toString())
                                .photo(attachment.getInputFile())
                                .parseMode("HTML")
                                .build(), context);
                    case VIDEO:
                        return new VideoMessageExecutor(SendVideo.builder()
                                .chatId(chatId)
                                .caption(sb.toString())
                                .video(attachment.getInputFile())
                                .parseMode("HTML")
                                .build(), context);
                    case AUDIO:
                        return new AudioMessageExecutor(SendAudio.builder()
                                .chatId(chatId)
                                .caption(sb.toString())
                                .audio(attachment.getInputFile())
                                .parseMode("HTML")
                                .build(), context);
                    case DOCUMENT:
                    case FILE:
                        return new DocumentMessageExecutor(SendDocument.builder()
                                .chatId(chatId)
                                .caption(sb.toString())
                                .document(attachment.getInputFile())
                                .parseMode("HTML")
                                .build(), context);
                    default:
                        SendMessage sendMessage = SendMessage.builder()
                                .chatId(chatId)
                                .text(sb.toString())
                                .parseMode("HTML")
                                .build();
                        return new MessageExecutor<>(sendMessage, context);
                }
            }
        }
    }

    public AbstractExecutor<Message> optionsMenu(@Nullable TelegramOptions telegramOptions) {
        InlineKeyboardButton oltTrackingButton = InlineKeyboardButton.builder()
                .text("Изменить отслеживание ОНУ")
                .callbackData(CallbackData.create("t_opt", "ch_track_olt"))
                .build();
        List<List<InlineKeyboardButton>> keyboard = List.of(
                List.of(oltTrackingButton)
        );

        StringBuilder sb = new StringBuilder(Decorator.bold("Настройки\n\n"));
        if (telegramOptions == null) {
            sb.append(Decorator.underline("Статусы ОНУ:")).append(" ").append(Decorator.bold("Не отслеживаются"));
            sb.append("\n");
        } else {
            sb.append(Decorator.underline("Статусы ОНУ:")).append(" ");
            if (telegramOptions.getTrackTerminal() == null) {
                sb.append(Decorator.bold("Не отслеживаются"));
            } else {
                if (telegramOptions.getTrackTerminal().equals("all")) {
                    sb.append(Decorator.bold("Все головы"));
                } else {
                    sb.append(Decorator.bold(telegramOptions.getTrackTerminal()));
                }
            }
            sb.append("\n");
        }

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(sb.toString())
                .parseMode(ParseMode.HTML)
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboard(keyboard)
                                .build()
                ).build();

        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> trackOltSettings(Employee employee, List<OpticalLineTerminal> oltList) {
        TelegramOptions telegramOptions = employee.getTelegramOptions();
        List<InlineKeyboardButton> keyboardButtons = new ArrayList<>();
        if (telegramOptions == null) {
            keyboardButtons.add(InlineKeyboardButton.builder()
                    .text("Все головы")
                    .callbackData(CallbackData.create("set_track_olt", "all"))
                    .build());
            keyboardButtons.addAll(oltList.stream()
                    .map(olt -> InlineKeyboardButton.builder()
                            .text(olt.getName() != null ? olt.getName() : olt.getIp())
                            .callbackData(CallbackData.create("set_track_olt", olt.getIp()))
                            .build()).toList());
        } else {
            if (telegramOptions.getTrackTerminal() != null)
                keyboardButtons.add(InlineKeyboardButton.builder()
                        .text("Откл. отслеживание")
                        .callbackData(CallbackData.create("set_track_olt", "null"))
                        .build());
            if (!Objects.equals(telegramOptions.getTrackTerminal(), "all"))
                keyboardButtons.add(InlineKeyboardButton.builder()
                        .text("Все головы")
                        .callbackData(CallbackData.create("set_track_olt", "all"))
                        .build());
            keyboardButtons.addAll(oltList.stream()
                    .filter(olt -> !Objects.equals(olt.getIp(), telegramOptions.getTrackTerminal()))
                    .map(olt -> InlineKeyboardButton.builder()
                            .text(olt.getName() != null ? olt.getName() : olt.getIp())
                            .callbackData(CallbackData.create("set_track_olt", olt.getIp()))
                            .build()).toList());
        }

        List<List<InlineKeyboardButton>> keyboard = keyboardButtons.stream().map(List::of).toList();

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(Decorator.bold("Какие головы отслеживать:\n"))
                .parseMode(ParseMode.HTML)
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboard(keyboard)
                                .build()
                ).build();

        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> sendOntEvents(List<OntStatusChangeEvent> events) {

        StringBuilder sb = new StringBuilder();

        OntStatusChangeEvent ontStatusChangeEvent = events.get(0);

        OpticalLineTerminal olt = ontStatusChangeEvent.getTerminal().getOlt();

        String oltName = olt.getName() != null ? olt.getName() : olt.getIp();

        Boolean isOnline = ontStatusChangeEvent.getIsOnline();
        String icon = isOnline ? "✅" : "❌";
        String status = isOnline ? "Up" : "Down";
        int terminalCount = events.size();

        sb.append(icon).append(" ");
        sb.append(Decorator.bold(oltName + " Порт: " + ontStatusChangeEvent.getTerminal().getPort())).append("\n");
        sb.append(Decorator.bold(status + " " + terminalCount + " ону")).append("\n");
        sb.append("\n");

        for (OntStatusChangeEvent event : events) {

            String ontName = (event.getTerminal().getDescription() != null && !event.getTerminal().getDescription().isBlank())
                    ? event.getTerminal().getDescription() : event.getTerminal().getMac();

            sb.append(event.getTerminal().getPosition()).append(": ").append(Decorator.bold(ontName));
            if (event.getIsOnline())
                sb.append(" ").append(String.format("%.2f", event.getTerminal().getCurRxSignal())).append(" dBm");
            sb.append("\n");
        }

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(sb.toString())
                .parseMode(ParseMode.HTML)
                .build();

        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> autoTariffs(List<AutoTariff> tariffs, String login, String employeeBillingMgr, Boolean isService) {
        List<InlineKeyboardButton> keyboardButtons = tariffs
                .stream()
                .map(autoTariff -> InlineKeyboardButton.builder()
                        .text(autoTariff.getName() + " " + autoTariff.getCost() + " р.")
                        .callbackData(CallbackData.create(isService ? "append_service" : "set_auto_tariff", autoTariff.getExternalId().toString(), login, employeeBillingMgr))
                        .build()).collect(Collectors.toList());

        keyboardButtons.add(InlineKeyboardButton.builder()
                .text("Отмена")
                .callbackData(CallbackData.create("cancel", ""))
                .build());

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(Decorator.bold(isService ? "Какой сервис добавить?" : "Какой тариф включить?"))
                .parseMode(ParseMode.HTML)
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboard(keyboardButtons.stream().map(List::of).toList())
                                .build()
                ).build();

        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> billingUserSetup(ApiBillingController.TotalUserInfo userInfo, Employee manager, Long taskClassId, String taskTypeId) {
        StringBuilder stringBuilder = new StringBuilder();

        String address = userInfo.getIbase().getAddr();
        String fio = userInfo.getIbase().getFio();
        String phone = userInfo.getIbase().getPhone();

        stringBuilder.append(Decorator.bold(userInfo.getUname())).append("\n");
        if (!address.isBlank()) stringBuilder.append(userInfo.getIbase().getAddr()).append("\n");
        if (!fio.isBlank()) stringBuilder.append(userInfo.getIbase().getFio()).append("\n");
        if (!phone.isBlank()) stringBuilder.append(userInfo.getIbase().getPhone()).append("\n");

        stringBuilder.append("\n").append(userInfo.getNewTarif().getUserStatusName()).append("\n");

        ApiBillingController.OldTarifItem mainTariff = userInfo.getOldTarif().get(0);
        if (mainTariff != null && !mainTariff.getService().isBlank()) {
            stringBuilder.append(Decorator.italic(mainTariff.getService())).append(" ").append(mainTariff.getPrice()).append("руб").append("\n");
        } else {
            stringBuilder.append(Decorator.italic("Нет тарифа")).append("\n");
        }

        if (userInfo.getOldTarif().size() > 1) {
            stringBuilder.append(Decorator.underline("Сервисы:\n"));
            for (int i = 1; i < userInfo.getOldTarif().size(); i++) {
                ApiBillingController.OldTarifItem service = userInfo.getOldTarif().get(i);
                stringBuilder.append(service.getService()).append(" ").append(service.getPrice()).append("руб").append("\n");
            }
        }

        stringBuilder.append(Decorator.bold("Баланс: "));
        Float balance = userInfo.getIbase().getMoney();
        if (balance != null) {
            stringBuilder.append(balance).append(" руб");
        } else {
            stringBuilder.append(Decorator.italic("0 руб"));
        }

        List<InlineKeyboardButton> firstRowButtons = new ArrayList<>();
        firstRowButtons.add(
                InlineKeyboardButton.builder()
                        .text("Изменить тариф")
                        .callbackData(CallbackData.create("change_tariff_menu", userInfo.getUname(), manager.getLogin())).build()
        );
        firstRowButtons.add(
                InlineKeyboardButton.builder()
                        .text("Добавить сервис")
                        .callbackData(CallbackData.create("append_service_menu", userInfo.getUname(), manager.getLogin())).build()
        );
        List<InlineKeyboardButton> secondRowButtons = new ArrayList<>();
        secondRowButtons.add(
                InlineKeyboardButton.builder()
                        .text("Отмена")
                        .callbackData(CallbackData.create("cancel", "")).build()
        );

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(stringBuilder.toString())
                .replyMarkup(
                        InlineKeyboardMarkup
                                .builder()
                                .keyboard(List.of(firstRowButtons, secondRowButtons))
                                .build()
                )
                .parseMode(ParseMode.HTML)
                .build();

        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> linkForUserAuth(Long telegramId, String secretKey) {
        String url = Decorator.url("страницу авторизации", "http://127.0.0.1:4412/tauth?id=" + telegramId + "&sc=" + secretKey);
        String text = "Для начала нужно перейти на " + url + " и ввести свой логин и пароль из договора";
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                .parseMode(ParseMode.HTML)
                .build();

        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> userMainMenu(String message) {
        KeyboardFactory keyboardFactory = new KeyboardFactory()
                .newLine("\uD83D\uDCC8 Активный тариф", "\uD83D\uDCFA Активные доп.услуги")
                .newLine("\uD83D\uDCB0 Проверить баланс")
                .newLine("\uD83D\uDED1 Приостановка обслуживания");

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .replyMarkup(keyboardFactory.getReplyKeyboard())
                .parseMode(ParseMode.HTML)
                .build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> userTariffRequest(ApiBillingController.TotalUserInfo userInfo) {
        String text = "Текущий тариф - " + Decorator.bold(userInfo.getNewTarif().getTarif())
                + "\nСо скоростью " + userInfo.getNewTarif().getTspeed() + " Mbit/s • "
                + userInfo.getOldTarif().get(0).getPrice() + " руб./п.";
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode(ParseMode.HTML)
                .build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> userServicesRequest(ApiBillingController.TotalUserInfo userInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        KeyboardFactory keyboardFactory = new KeyboardFactory()
                .newLine("✅ Подключить доп.услугу");

        if(userInfo.getOldTarif().size() <= 1) {
            stringBuilder.append(Decorator.italic("Доп.услуги не подключены"));
        }else{
            keyboardFactory.newLine("❌ Отключить доп.услугу");
            stringBuilder.append("Доп.услуги:\n");
            for (int i = 1; i < userInfo.getOldTarif().size(); i++) {
                ApiBillingController.OldTarifItem service = userInfo.getOldTarif().get(i);
                stringBuilder.append(Decorator.bold(service.getService())).append(" • ").append(service.getPrice()).append(" руб./п.").append("\n");
            }
        }

        keyboardFactory.newLine("◀\uFE0F Главное меню");

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(stringBuilder.toString())
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardFactory.getReplyKeyboard())
                .build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> connectUserService(ApiBillingController.TotalUserInfo userInfo, List<UserTariff> services) {
        KeyboardFactory keyboardFactory  = new KeyboardFactory();
        services.forEach(service -> keyboardFactory.newLine(KeyboardFactory.IKButton.of(service.getName() + " " + service.getPriceLabel(), "connect_user_service", service.getBaseName())));
        SendMessage sendMessage  = SendMessage.builder()
                .chatId(chatId)
                .text("Какую услугу подключить?")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardFactory.getInlineKeyboard())
                .build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Message> disconnectUserService(ApiBillingController.TotalUserInfo userInfo) {
        final KeyboardFactory keyboardFactory   = new KeyboardFactory();
        List<ApiBillingController.OldTarifItem> connectedServices  = userInfo.getOldTarif().stream().skip(1).toList();
        connectedServices.forEach(service  -> keyboardFactory.newLine(KeyboardFactory.IKButton.of(service.getService(),  "disconnect_user_service", service.getService())));
        SendMessage sendMessage   = SendMessage.builder()
                .chatId(chatId)
                .text("Какую услугу отключить?")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardFactory.getInlineKeyboard())
                .build();
        return new MessageExecutor<>(sendMessage, context);
    }

    public AbstractExecutor<Serializable> userRequestResponse(Integer messageId, String text) {
        KeyboardFactory keyboardFactory   = new KeyboardFactory();
        keyboardFactory.newLine(KeyboardFactory.IKButton.of("Посмотреть активные запросы", "get_user_requests", ""));
        return new MessageExecutor<>(EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .replyMarkup(keyboardFactory.getInlineKeyboard())
                .parseMode("HTML")
                .build(), context);
    }

    /**
     * Позволяет отправить запрос в Telegram API и получить результат согласно заданному контексту
     *
     * @param <T> Возвращаемое значение от Telegram API
     */
    public interface AbstractExecutor<T> {
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

    public static class GroupMessageExecutor implements AbstractExecutor<Message> {
        private final SendMediaGroup message;
        private final MainBot context;

        public GroupMessageExecutor(SendMediaGroup message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Message execute() throws TelegramApiException {
            return context.execute(message).get(0);
        }
    }

    public static class MultiGroupMessageExecutor implements AbstractExecutor<Message> {
        private final SendMediaGroup visualGroup;
        private final SendMediaGroup audioGroup;
        private final SendMediaGroup filesGroup;
        private final MainBot context;

        public MultiGroupMessageExecutor(SendMediaGroup visualGroup, SendMediaGroup audioGroup, SendMediaGroup filesGroup, MainBot context) {
            this.visualGroup = visualGroup;
            this.audioGroup = audioGroup;
            this.filesGroup = filesGroup;

            this.context = context;
        }

        public Message execute() throws TelegramApiException {
            Message message = null;
            if (visualGroup != null) message = context.execute(visualGroup).get(0);
            if (audioGroup != null) message = context.execute(audioGroup).get(0);
            if (filesGroup != null) message = context.execute(filesGroup).get(0);
            return message;
        }
    }

    public static class MultiMessageExecutor implements AbstractExecutor<Message> {
        private final List<PartialBotApiMethod<?>> messages;
        private final MainBot context;

        public MultiMessageExecutor(List<PartialBotApiMethod<?>> messages, MainBot context) {
            this.messages = messages;
            this.context = context;
        }

        public Message execute() throws TelegramApiException {
            Message sentMsg = null;
            for (PartialBotApiMethod<?> message : messages) {
                if (message instanceof SendMessage) {
                    sentMsg = context.execute((SendMessage) message);
                }
                if (message instanceof SendPhoto) {
                    sentMsg = context.execute((SendPhoto) message);
                }
                if (message instanceof SendVideo) {
                    sentMsg = context.execute((SendVideo) message);
                }
                if (message instanceof SendAudio) {
                    sentMsg = context.execute((SendAudio) message);
                }
                if (message instanceof SendDocument) {
                    sentMsg = context.execute((SendDocument) message);
                }
                if (message instanceof SendMediaGroup) {
                    sentMsg = context.execute((SendMediaGroup) message).get(0);
                }
            }
            return sentMsg;
        }
    }

    public static class CallbackAnswerExecutor implements AbstractExecutor<Serializable> {
        private final AnswerCallbackQuery message;
        private final MainBot context;

        public CallbackAnswerExecutor(AnswerCallbackQuery message, MainBot context) {
            this.message = message;
            this.context = context;
        }

        public Serializable execute() throws TelegramApiException {
            return context.execute(message);
        }
    }
}
