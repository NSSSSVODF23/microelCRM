package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.CustomException;
import com.microel.trackerbackend.controllers.configuration.ConfigurationStorage;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.controllers.telegram.handle.Decorator;
import com.microel.trackerbackend.controllers.telegram.reactor.*;
import com.microel.trackerbackend.misc.DhcpIpRequestNotificationBody;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.services.FilesWatchService;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.RestPage;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.SwitchBaseInfo;
import com.microel.trackerbackend.services.external.billing.BillingRequestController;
import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.dispatchers.*;
import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.dto.mapper.ChatMapper;
import com.microel.trackerbackend.storage.entities.address.House;
import com.microel.trackerbackend.storage.entities.chat.Chat;
import com.microel.trackerbackend.storage.entities.chat.*;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.filesys.TFile;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.task.WorkLogTargetFile;
import com.microel.trackerbackend.storage.entities.task.utils.AcceptingEntry;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.entities.templating.model.ModelItem;
import com.microel.trackerbackend.storage.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static java.util.Comparator.comparingInt;

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
    private final BillingRequestController billingRequestController;
    private final AddressDispatcher addressDispatcher;
    private final HouseDispatcher houseDispatcher;
    private final WorkingDayDispatcher workingDayDispatcher;
    private final AcpClient acpClient;
    private final FilesWatchService filesWatchService;
    private final Map<String, List<Message>> groupedMessagesFromTelegram = new ConcurrentHashMap<>();
    private final Map<Employee, OperatingMode> operatingModes = new ConcurrentHashMap<>();
    private final Map<Employee, List<Message>> reportMessages = new ConcurrentHashMap<>();
    private TelegramConf telegramConf;
    @Nullable
    private TelegramBotsApi api;
    @Nullable
    private MainBot mainBot;
    @Nullable
    private BotSession mainBotSession;

    public TelegramController(ConfigurationStorage configurationStorage, TaskDispatcher taskDispatcher, WorkLogDispatcher workLogDispatcher,
                              ChatDispatcher chatDispatcher, ChatMessageDispatcher chatMessageDispatcher, EmployeeDispatcher employeeDispatcher,
                              StompController stompController, AttachmentDispatcher attachmentDispatcher, BillingRequestController billingRequestController,
                              AddressDispatcher addressDispatcher, HouseDispatcher houseDispatcher, WorkingDayDispatcher workingDayDispatcher, AcpClient acpClient, FilesWatchService filesWatchService) {
        this.configurationStorage = configurationStorage;
        this.taskDispatcher = taskDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.chatDispatcher = chatDispatcher;
        this.chatMessageDispatcher = chatMessageDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
        this.attachmentDispatcher = attachmentDispatcher;
        this.billingRequestController = billingRequestController;
        this.addressDispatcher = addressDispatcher;
        this.houseDispatcher = houseDispatcher;
        this.workingDayDispatcher = workingDayDispatcher;
        this.acpClient = acpClient;
        this.filesWatchService = filesWatchService;
        try {
            telegramConf = configurationStorage.load(TelegramConf.class);
            initializeMainBot();
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

    public void initializeMainBot() throws TelegramApiException, IOException {
        initializeApi();
        if (api == null) throw new IOException("Telegram API не инициализировано");
        if (mainBotSession != null && mainBotSession.isRunning()) mainBotSession.stop();
        mainBot = new MainBot(telegramConf);
        initializeChatCommands();

        mainBotSession = api.registerBot(mainBot);
    }

    private void initializeChatCommands() throws IOException, TelegramApiException {
        if (mainBot == null) throw new IOException("Telegram Bot не инициализирован");
        List<BotCommand> commands = List.of(
                new BotCommand("current_task", "Текущая задача"),
                new BotCommand("another_task", "Список моих задач"),
                new BotCommand("check_alive", "Проверить живых"),
                new BotCommand("house_sessions", "Получить сессии в доме"),
                new BotCommand("commutator_sessions", "Получить сессии коммутатора"),
                new BotCommand("search_schemes", "Найти схему")
        );
        SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(commands)
                .build();
        mainBot.execute(setMyCommands);

        mainBot.subscribe(new TelegramChatJoinReactor(update -> {
            System.out.println(update);
            return true;
        }));

        mainBot.subscribe(new TelegramCommandReactor("/groupid", update -> {
            Message message = update.getMessage();
            TelegramMessageFactory.create(message.getChatId(), mainBot).groupIdResponse().execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCommandReactor("/start", update -> {
            Message message = update.getMessage();
            TelegramMessageFactory.create(message.getChatId(), mainBot).userIdResponse().execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCommandReactor("/mytasks", update -> {
            Long chatId = update.getMessage().getChatId();
            return true;
        }));

//        mainBot.subscribe(new TelegramCommandReactor("/menu", update -> {
//            Long chatId = update.getMessage().getChatId();
//            try {
//                Employee employee = getEmployeeByChat(chatId);
//                operatingModes.put(employee, OperatingMode.CHECK_ALIVE);
//                if (employee.getOffsite()) {
//                    TelegramMessageFactory.create(chatId, mainBot).offsiteMenu().execute();
//                } else {
//                    TelegramMessageFactory.create(chatId, mainBot).simpleMessage("Меню отсутствует").execute();
//                }
//                return true;
//            } catch (Exception e) {
//                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
//                return false;
//            }
//        }));

        mainBot.subscribe(new TelegramCommandReactor("/current_task", update -> {
            Long chatId = update.getMessage().getChatId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                WorkLog acceptedByTelegramId = workLogDispatcher.getAcceptedByTelegramIdDTO(chatId);
                TelegramMessageFactory.create(chatId, mainBot).currentActiveTask(acceptedByTelegramId.getTask()).execute();
                return true;
            } catch (Exception e) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCommandReactor("/another_task", update -> {
            Long chatId = update.getMessage().getChatId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                return sendWorkLogQueue(chatId);
            } catch (Exception e) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCommandReactor("/check_alive", update -> {
            Long chatId = update.getMessage().getChatId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                operatingModes.put(employee, OperatingMode.CHECK_ALIVE);
                TelegramMessageFactory.create(chatId, mainBot).infoModeCancel("Введите адрес дома:", "check_alive").execute();
                return true;
            } catch (Exception e) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCommandReactor("/house_sessions", update -> {
            Long chatId = update.getMessage().getChatId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                operatingModes.put(employee, OperatingMode.HOUSE_SESSIONS);
                TelegramMessageFactory.create(chatId, mainBot).infoModeCancel("Введите адрес дома:", "house_sessions").execute();
                return true;
            } catch (Exception e) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCommandReactor("/commutator_sessions", update -> {
            Long chatId = update.getMessage().getChatId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                operatingModes.put(employee, OperatingMode.COMMUTATOR_SESSIONS);
                TelegramMessageFactory.create(chatId, mainBot).infoModeCancel("Введите адрес дома:", "commutator_sessions").execute();
                return true;
            } catch (Exception e) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCommandReactor("/search_schemes", update -> {
            Long chatId = update.getMessage().getChatId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                operatingModes.put(employee, OperatingMode.SEARCH_FILES);
                TelegramMessageFactory.create(chatId, mainBot).infoModeCancel("Введите название схемы:", "search_schemes").execute();
                return true;
            } catch (Exception e) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCommandReactor("/dhcpnotificationgroup", update -> {
            Long chatId = update.getMessage().getChatId();
            Boolean isGroup = update.getMessage().getChat().isGroupChat();
            if (!isGroup) {
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage("Чат не является группой").execute();
                return false;
            }
            telegramConf = configurationStorage.loadOrDefault(TelegramConf.class, new TelegramConf());
            telegramConf.setDhcpNotificationChatId(chatId.toString());
            configurationStorage.save(telegramConf);
            stompController.changeTelegramConfig(telegramConf);
            TelegramMessageFactory.create(chatId, mainBot).simpleMessage("Установлена группа для получения уведомлений о DHCP").execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("main_menu", (u, data) -> {
            if (data == null) return false;
            Long chatId = u.getCallbackQuery().getMessage().getChatId();
            Integer messageId = u.getCallbackQuery().getMessage().getMessageId();
            if (data.isData("active_task")) {
                mainBot.send(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                WorkLog acceptedByTelegramId = workLogDispatcher.getAcceptedByTelegramIdDTO(chatId);
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
            String callbackId = update.getCallbackQuery().getId();

            WorkLog workLog = null;

            try {
                workLog = workLogDispatcher.acceptWorkLog(data.getLong(), chatId);
            }catch (CustomException e){
                TelegramMessageFactory.create(chatId, mainBot).answerCallback(callbackId, e.getMessage()).execute();
                return false;
            }

            EditMessageReplyMarkup clearMarkup = EditMessageReplyMarkup.builder().chatId(chatId).messageId(messageId)
                    .replyMarkup(InlineKeyboardMarkup.builder().clearKeyboard().build()).build();
            mainBot.send(clearMarkup);
            ChatDto chatFromWorkLog = workLogDispatcher.getChatByWorkLogId(data.getLong());
            Employee employee = employeeDispatcher.getByTelegramId(chatId).orElseThrow(() -> new EntryNotFound("Пользователь не найден по идентификатору Telegram Api"));
            SuperMessage systemMessage = chatDispatcher.createSystemMessage(chatFromWorkLog.getChatId(), "\uD83D\uDC77\uD83C\uDFFB\u200D♂️" + employee.getFullName() + " принял задачу и подключился к чату", this);
            String targetDescription = workLog.getTargetDescription();
            List<WorkLogTargetFile> files = workLog.getTargetFiles();
            // Отправляем обновления в интерфейс пользователя
            broadcastUpdatesToWeb(systemMessage);
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            messageFactory.currentActiveTask(workLog.getTask()).execute();
            if(employee.isHasGroup()){
                Set<String> employeesWithEqualGroups = workLog.getEmployees().stream()
                        .filter(Employee::isHasGroup)
                        .filter(emp->Objects.equals(employee.getTelegramGroupChatId(), emp.getTelegramGroupChatId()))
                        .map(Employee::getLogin)
                        .collect(Collectors.toSet());
                boolean alreadySentTaskInfoToGroup = workLog.getAcceptedEmployees().stream()
                        .map(AcceptingEntry::getLogin)
                        .filter(login -> !Objects.equals(login, employee.getLogin()))
                        .anyMatch(employeesWithEqualGroups::contains);
                if(!alreadySentTaskInfoToGroup || workLog.getGangLeader() != null) {
                    TelegramMessageFactory groupChatFactory = TelegramMessageFactory.create(employee.getTelegramGroupChatId(), mainBot);
                    groupChatFactory.currentActiveTaskForGroupChat(workLog.getTask()).execute();
                    if (files != null && !files.isEmpty()) {
                        if(files.size() == 1){
                            groupChatFactory.workTargetMessage(targetDescription, files.get(0)).execute();
                        }else{
                            groupChatFactory.workTargetGroupMessage(targetDescription, files).execute();
                        }
                    }else if (targetDescription != null && !targetDescription.isBlank()){
                        groupChatFactory.workTargetMessage(targetDescription, null).execute();
                    }
                }
            }
            if (files != null && !files.isEmpty()) {
                if(files.size() == 1){
                    messageFactory.workTargetMessage(targetDescription, files.get(0)).execute();
                }else{
                    messageFactory.workTargetGroupMessage(targetDescription, files).execute();
                }
            }else if (targetDescription != null && !targetDescription.isBlank()){
                messageFactory.workTargetMessage(targetDescription, null).execute();
            }
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("send_report", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackId = update.getCallbackQuery().getId();
            try{
                Employee employee = getEmployeeByChat(chatId);

                TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

                if (operatingModes.get(employee) == null) {
                    factory.deleteMessage(messageId).execute();
                    factory.answerCallback(callbackId, "Вы не можете отправить отчет, нужно быть в режиме закрытия задачи.").execute();
                    return true;
                } else if (reportMessages.get(employee).isEmpty()) {
                    factory.answerCallback(callbackId, "Перед закрытием нужно отправить сообщение (или несколько), с отчетом. Отчет о выполненных работах не может быть пуст.").execute();
                    return false;
                }
                try {
                    List<Message> messageList = reportMessages.get(employee);
                    WorkLog workLog = workLogDispatcher.createReport(employee, messageList);
                    if(employee.isHasGroup()){
                        String title = Decorator.underline(Decorator.bold("Отчет "+employee.getFullName()+" по задаче #"+workLog.getTask().getTaskId()));
                        String reportText = messageList.stream().map(Message::getText).collect(Collectors.joining("\n"));
                        TelegramMessageFactory.create(employee.getTelegramGroupChatId(), mainBot)
                                .simpleMessage(title+"\n"+reportText)
                                .execute();
                    }
                    reportMessages.remove(employee);
                    operatingModes.remove(employee);
                    factory.deleteMessage(messageId).execute();
                    factory.simpleMessage("Отчет успешно отправлен").execute();
//                    sendTextBroadcastMessage(workLog.getChat(), ChatMessage.of("Написал отчет и отключился от чата задачи.", employee));
                    return sendWorkLogQueue(chatId);
                } catch (EntryNotFound | IllegalFields e) {
                    factory.deleteMessage(messageId).execute();
                    factory.simpleMessage(e.getMessage()).execute();
                    return false;
                }
            }catch (Exception e){
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage() == null ? e.getMessage() : "Неизвестная ошибка").execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("cancel_mode", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            try {
                Employee employee = getEmployeeByChat(chatId);
                if(data == null){
                    messageFactory.answerCallback(callbackId, "Пустое сообщение о выходе из режима").execute();
                    return false;
                }
                switch (data.getString()){
                    case "check_alive"-> {
                        operatingModes.remove(employee);
                        messageFactory.answerCallback(callbackId, "Выход из режима проверки живых абонентов").execute();
                        return true;
                    }
                    case "house_sessions" ->{
                        operatingModes.remove(employee);
                        messageFactory.answerCallback(callbackId,"Выход из режима сессий в доме").execute();
                        return true;
                    }
                    case "commutator_sessions" ->{
                        operatingModes.remove(employee);
                        messageFactory.answerCallback(callbackId,"Выход из режима сессий в коммутаторе").execute();
                        return true;
                    }
                    case "search_schemes" -> {
                        operatingModes.remove(employee);
                        messageFactory.answerCallback(callbackId,"Выход из режима поиска схем").execute();
                        return true;
                    }
                    default -> messageFactory.answerCallback(callbackId,"Неизвестный режим").execute();
                }
                return false;
            }catch (Exception e){
                messageFactory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("cancel_close", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                mainBot.send(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                if (Objects.equals(operatingModes.get(employee), OperatingMode.REPORT_SENDING)) {
                    reportMessages.remove(employee);
                    operatingModes.remove(employee);
                    WorkLog workLog = workLogDispatcher.getAcceptedByTelegramIdDTO(chatId);
                    TelegramMessageFactory.create(chatId, mainBot).currentActiveTask(workLog.getTask()).execute();
                    TelegramMessageFactory.create(chatId, mainBot).answerCallback(callbackId, "Вы отменили завершение задачи, она вновь активна. Вы находитесь в режиме чата задачи.").execute();
                    return true;
                }
                TelegramMessageFactory.create(chatId, mainBot).answerCallback(callbackId,"Вы не можете отменить завершение задачи так как не находитесь в режиме закрытия задачи.").execute();
                return false;
            }catch (Exception e){
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("check_alive_address", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data == null) {
                messageFactory.answerCallback(callbackId,"Неверные данные callback").execute();
                return false;
            }
            try {
                Employee employee = getEmployeeByChat(chatId);
                try {
                    House house = houseDispatcher.get(data.getLong());
                    String calculateCountingLives = billingRequestController.getCalculateCountingLives(BillingRequestController.CountingLivesForm.of(house.getAddress(), 1, 500));
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.simpleMessage(calculateCountingLives).execute();
                    operatingModes.remove(employee);
                }catch (Exception e){
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.simpleMessage("Ошибка: "+e.getMessage()).execute();
                    return false;
                }
                return true;
            }catch (Exception e){
                messageFactory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("house_sessions_address", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data == null) {
                messageFactory.answerCallback(callbackId,"Неверные данные callback").execute();
                return false;
            }
            try {
                Employee employee = getEmployeeByChat(chatId);
                try {
                    House house = houseDispatcher.get(data.getLong());
                    if(house.getAcpHouseBind() == null){
                        messageFactory.answerCallback(callbackId, "Адрес не связан с ACP").execute();
                        return false;
                    }
                    Integer buildingId = house.getAcpHouseBind().getBuildingId();
                    RestPage<DhcpBinding> lastBindings = acpClient.getLastBindings(0, (short) 1, null, null, null, null, buildingId, null);
                    lastBindings.forEach(lb->{
                        BillingRequestController.TotalUserInfo userInfo = billingRequestController.getUserInfo(lb.getAuthName());
                        if(userInfo != null){
                            lb.setBillingAddress(userInfo.getIbase().getAddr());
                        }
                    });
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.sessionPage(lastBindings, buildingId).execute();
                    operatingModes.remove(employee);
                }catch (Exception e){
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.simpleMessage("Ошибка: "+e.getMessage()).execute();
                    return false;
                }
                return true;
            }catch (Exception e){
                messageFactory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("commutator_sessions_com_sel", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data == null) {
                messageFactory.answerCallback(callbackId,"Неверные данные callback").execute();
                return false;
            }
            try {
                Employee employee = getEmployeeByChat(chatId);
                try {
                    RestPage<DhcpBinding> lastBindings = acpClient.getLastBindings(0, (short) 1, null, null, null, null, null, data.getInt(), null);
                    lastBindings.forEach(lb->{
                        BillingRequestController.TotalUserInfo userInfo = billingRequestController.getUserInfo(lb.getAuthName());
                        if(userInfo != null){
                            lb.setBillingAddress(userInfo.getIbase().getAddr());
                        }
                    });
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.sessionCommutatorPage(lastBindings, data.getInt()).execute();
                    operatingModes.remove(employee);
                }catch (Exception e){
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.simpleMessage("Ошибка: "+e.getMessage()).execute();
                    return false;
                }
                return true;
            }catch (Exception e){
                messageFactory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("commutator_sessions_address", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data == null) {
                messageFactory.answerCallback(callbackId,"Неверные данные callback").execute();
                return false;
            }
            try {
                Employee employee = getEmployeeByChat(chatId);
                try {
                    House house = houseDispatcher.get(data.getLong());
                    if(house.getAcpHouseBind() == null){
                        messageFactory.answerCallback(callbackId, "Адрес не связан с ACP").execute();
                        return false;
                    }
                    Integer buildingId = house.getAcpHouseBind().getBuildingId();
                    Page<SwitchBaseInfo> commutators = acpClient.getCommutators(0, null, null, buildingId, 15);
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.commutatorSessions(commutators).execute();
                    operatingModes.remove(employee);
                }catch (Exception e){
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.simpleMessage("Ошибка: "+e.getMessage()).execute();
                    return false;
                }
                return true;
            }catch (Exception e){
                messageFactory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramCallbackReactor("load_page", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data != null){
                String[] split = data.getString().split(":");
                if(split.length>=2){
                    switch (split[0]){
                        case "sessionHouse"-> {
                            Integer pageHouse = Integer.parseInt(split[2]);
                            Integer buildingId = Integer.parseInt(split[1]);
                            RestPage<DhcpBinding> lastBindings = acpClient.getLastBindings(pageHouse, (short) 1, null, null, null, null, buildingId, null);
                            lastBindings.forEach(lb->{
                                BillingRequestController.TotalUserInfo userInfo = billingRequestController.getUserInfo(lb.getAuthName());
                                if(userInfo != null){
                                    lb.setBillingAddress(userInfo.getIbase().getAddr());
                                }
                            });
                            messageFactory.answerCallback(callbackId, null).execute();
                            messageFactory.sessionPage(lastBindings, buildingId).execute();
                            return true;
                        }
                        case "sessionHouseCommutator"->{
                            Integer pageCommutator = Integer.parseInt(split[2]);
                            Integer commutatorId = Integer.parseInt(split[1]);
                            RestPage<DhcpBinding> lastBindingsCommutator = acpClient.getLastBindings(pageCommutator, null, null, null, null, null, null, commutatorId, null);
                            lastBindingsCommutator.forEach(lb->{
                                BillingRequestController.TotalUserInfo userInfo = billingRequestController.getUserInfo(lb.getAuthName());
                                if(userInfo != null){
                                    lb.setBillingAddress(userInfo.getIbase().getAddr());
                                }
                            });
                            messageFactory.answerCallback(callbackId, null).execute();
                            messageFactory.sessionCommutatorPage(lastBindingsCommutator, commutatorId).execute();
                            return true;
                        }
                    }
                }else{
                    messageFactory.answerCallback(callbackId,"Не верный формат callback");
                }
            }
            return false;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("get_billing_info", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data != null){
                try {
                    Employee employee = getEmployeeByChat(chatId);
                    try {
                        BillingRequestController.TotalUserInfo userInfo = billingRequestController.getUserInfo(data.getString());
                        messageFactory.answerCallback(callbackId, null).execute();
                        messageFactory.billingInfo(userInfo).execute();
                        return true;
                    }catch (EmptyResponse e){
                        messageFactory.answerCallback(callbackId, "Не удалось найти информацию о пользователе").execute();
                    }
                }catch (Exception e){
                    messageFactory.answerCallback(callbackId, e.getMessage()).execute();
                }
            }
            return false;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("get_user_hardware", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data != null){
                try {
                    Employee employee = getEmployeeByChat(chatId);
                    List<DhcpBinding> bindingsByLogin = acpClient.getBindingsByLogin(data.getString());
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.sessionPage(bindingsByLogin).execute();
                    return true;
                }catch (Exception e){
                    messageFactory.answerCallback(callbackId, e.getMessage()).execute();
                }
            }
            return false;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("get_file", (update, data) -> {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackId = update.getCallbackQuery().getId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            if(data != null){
                try {
                    Employee employee = getEmployeeByChat(chatId);
                    TFile file = filesWatchService.getFileById(data.getLong()).orElse(null);
                    if(file == null){
                        messageFactory.answerCallback(callbackId, "Файл не найден").execute();
                        return false;
                    }
                    messageFactory.answerCallback(callbackId, null).execute();
                    messageFactory.file(file).execute();
                    operatingModes.remove(employee);
                    return true;
                }catch (Exception e){
                    messageFactory.answerCallback(callbackId, e.getMessage()).execute();
                }
            }
            return false;
        }));

//        mainBot.subscribe(new TelegramCallbackReactor("get_auth_variants", (update, data) -> {
//            Long chatId = update.getCallbackQuery().getMessage().getChatId();
//            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
//            String callbackId = update.getCallbackQuery().getId();
//            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
//            if(data != null){
//                try {
//                    Employee employee = getEmployeeByChat(chatId);
//                    messageFactory.answerCallback(callbackId, null).execute();
//                    messageFactory.authVariants(data.getString()).execute();
//                    return true;
//                }catch (Exception e){
//                    messageFactory.answerCallback(callbackId, e.getMessage()).execute();
//                }
//            }
//            return false;
//        }));
//
//        mainBot.subscribe(new TelegramCallbackReactor("auth_recently", (update, data) -> {
//            Long chatId = update.getCallbackQuery().getMessage().getChatId();
//            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
//            String callbackId = update.getCallbackQuery().getId();
//            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
//            if(data != null){
//                try {
//                    Employee employee = getEmployeeByChat(chatId);
//                    List<DhcpBinding> content = new ArrayList<>(acpClient.getLastBindings(0, (short) 1, null, null, null, null, null, null).getContent());
//                    reverse(content);
//                    messageFactory.answerCallback(callbackId, null).execute();
//                    for (DhcpBinding binding : content){
//                        messageFactory.authButtonList(binding, data.getString()).execute();
//                    }
//                    return true;
//                }catch (Exception e){
//                    messageFactory.answerCallback(callbackId, e.getMessage()).execute();
//                }
//            }
//            return false;
//        }));
//
//        mainBot.subscribe(new TelegramCallbackReactor("auth_by_mac", (update, data) -> {
//            Long chatId = update.getCallbackQuery().getMessage().getChatId();
//            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
//            String callbackId = update.getCallbackQuery().getId();
//            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
//            if(data != null){
//                try {
//                    Employee employee = getEmployeeByChat(chatId);
//                    List<DhcpBinding> content = new ArrayList<>(acpClient.getLastBindings(0, (short) 1, null, null, null, null, null, null)
//                            .getContent());
//                    reverse(content);
//                    messageFactory.answerCallback(callbackId, null).execute();
//                    for (DhcpBinding binding : content){
//                        messageFactory.authButtonList(binding, data.getString()).execute();
//                    }
//                    return true;
//                }catch (Exception e){
//                    messageFactory.answerCallback(callbackId, e.getMessage()).execute();
//                }
//            }
//            return false;
//        }));
//
//        mainBot.subscribe(new TelegramCallbackReactor("auth_login", (update, data) -> {
//            Long chatId = update.getCallbackQuery().getMessage().getChatId();
//            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
//            String callbackId = update.getCallbackQuery().getId();
//            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
//            if(data != null){
//                try {
//                    Employee employee = getEmployeeByChat(chatId);
//                    String[] splitData = data.getString().split("#");
//                    if(splitData.length != 2) {
//                        messageFactory.answerCallback(callbackId, "Не верные данные авторизации").execute();
//                        return false;
//                    }
//                    String login = splitData[0];
//                    String mac = splitData[1];
//                    DhcpBinding.AuthForm authForm = new DhcpBinding.AuthForm();
//                    authForm.setLogin(login);
//                    authForm.setMacaddr(mac);
//                    acpClient.authDhcpBinding(authForm);
//                    messageFactory.answerCallback(callbackId, "Логин " + login + " успешно авторизован под " + mac).execute();
//                    return true;
//                }catch (Exception e){
//                    messageFactory.answerCallback(callbackId, e.getMessage()).execute();
//                }
//            }
//            return false;
//        }));

        mainBot.subscribe(new TelegramPromptReactor("ℹ️ Меню задачи", (update) -> {
            Long chatId = update.getMessage().getChatId();
            TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            try {
                Employee employee = getEmployeeByChat(chatId);
                List<ModelItem> fields = workLogDispatcher.getTaskFieldsAcceptedWorkLog(employee);
                factory.loginInfoMenu(fields).execute();
                return true;
            }catch (Exception e){
                factory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramPromptReactor("\uD83D\uDC4C Завершить задачу", (update) -> {
            // Получаем активную задачу по идентификатору чата
            Long chatId = update.getMessage().getChatId();
            TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            try {
                Employee employee = getEmployeeByChat(chatId);
                try {
                    WorkLog workLog = workLogDispatcher.getAcceptedWorkLogByEmployee(employee);

                    if(workLog.getDeferredReport() != null && workLog.getDeferredReport()){
                        workLogDispatcher.createReport(employee);
                        if(employee.isHasGroup()){
                            String title = Decorator.underline(Decorator.bold("Задача завершена #"+workLog.getTask().getTaskId()));
                            TelegramMessageFactory.create(employee.getTelegramGroupChatId(), mainBot)
                                    .simpleMessage(title)
                                    .execute();
                        }
                        factory.simpleMessage("Задача завершена. Отчет нужно написать позже.").execute();
                        factory.clearKeyboardMenu().execute();
                        return true;
                    }

                    factory.closeWorkLogMessage().execute();
                    factory.clearKeyboardMenu().execute();
                    reportMessages.put(employee, new ArrayList<>());
                    operatingModes.put(employee, OperatingMode.REPORT_SENDING);
                    return true;
                } catch (EntryNotFound e) {
                    factory.simpleMessage("В данный момент у вас нет активных задач").execute();
                    factory.clearKeyboardMenu().execute();
                }
                return false;
            }catch (Exception e){
                factory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        // Обработка простых текстовых сообщений полученных из чата
        mainBot.subscribe(new TelegramMessageReactor(update -> {
            Long chatId = update.getMessage().getChatId();
            TelegramMessageFactory messageFactory = TelegramMessageFactory.create(chatId, mainBot);
            try {
                Employee employee = getEmployeeByChat(chatId);
                OperatingMode operatingMode = operatingModes.get(employee);
                if(operatingMode == null) {
                    String messageText = update.getMessage().getText();
                    if(messageText.equals("Зарплата")){
                        Integer salarySumByDateRange = workingDayDispatcher.getSalarySumByDateRange(employee, DateRange.thisMonth());
                        messageFactory.simpleMessage("Зарплата за текущий месяц: "+Decorator.bold(salarySumByDateRange+" руб.")).execute();
                        return true;
                    }
                    return false;
                }
                switch (operatingMode){
                    case REPORT_SENDING -> {
                        reportMessages.compute(employee, (key, value)->{
                            if (value == null) value = new ArrayList<>();
                            value.add(update.getMessage());
                            return value;
                        });
                        return true;
                    }
                    case CHECK_ALIVE -> {
                        List<House> suggestions = addressDispatcher.getSuggestionsHouse(update.getMessage().getText(), true).stream().limit(5).toList();
                        if(suggestions.isEmpty()){
                            messageFactory.simpleMessage("Адресов по запросу не найдено. Попробуйте еще раз.").execute();
                            return true;
                        }
                        messageFactory.addressSuggestions(suggestions, "check_alive_address").execute();
                        return true;
                    }
                    case HOUSE_SESSIONS -> {
                        List<House> suggestions = addressDispatcher.getSuggestionsHouse(update.getMessage().getText(), true).stream().limit(5).toList();
                        if(suggestions.isEmpty()){
                            messageFactory.simpleMessage("Адресов по запросу не найдено. Попробуйте еще раз.").execute();
                            return true;
                        }
                        messageFactory.addressSuggestions(suggestions, "house_sessions_address").execute();
                        return true;
                    }
                    case COMMUTATOR_SESSIONS -> {
                        List<House> suggestions = addressDispatcher.getSuggestionsHouse(update.getMessage().getText(), true).stream().limit(5).toList();
                        if(suggestions.isEmpty()){
                            messageFactory.simpleMessage("Адресов по запросу не найдено. Попробуйте еще раз.").execute();
                            return true;
                        }
                        messageFactory.addressSuggestions(suggestions, "commutator_sessions_address").execute();
                        return true;
                    }
                    case SEARCH_FILES -> {
                        List<TFile.FileSuggestion> foundFiles = filesWatchService.getFileSuggestions(update.getMessage().getText())
                                .stream().toList();
                        if(foundFiles.isEmpty()){
                            messageFactory.simpleMessage("Схем по запросу не найдено. Попробуйте еще раз.").execute();
                            return true;
                        }
                        messageFactory.fileSuggestions(foundFiles).execute();
                    }
                }
                return false;
            }catch (Exception e){
                messageFactory.simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramEditMessageReactor(update -> {
            Long chatId = update.getEditedMessage().getChatId();
            try {
                Employee employee = getEmployeeByChat(chatId);
                OperatingMode operatingMode = operatingModes.get(employee);
                if(operatingMode == null) {
//                    try {
//                        sendMessageFromTlgChat(update.getMessage());
//                        return true;
//                    } catch (IllegalFields | SaveEntryFailed | IllegalMediaType | ExceptionInsideThread e) {
//                        log.warn(e.getMessage());
//                    }
                    return false;
                }
                switch (operatingMode){
                    case REPORT_SENDING -> {
                        Message updateMessage = update.getEditedMessage();
                        reportMessages.computeIfPresent(employee, (key, value)->{
                            value.stream().filter(msg->msg.getMessageId().equals(updateMessage.getMessageId())).findFirst().ifPresent(msg->{
                                msg.setText(updateMessage.getText());
                            });
                            return value;
                        });
                        return true;
                    }
                    case CHECK_ALIVE -> {
                        return true;
                    }
                }
                return false;
            }catch (Exception e){
                TelegramMessageFactory.create(chatId, mainBot).simpleMessage(e.getMessage()).execute();
                return false;
            }
        }));

        mainBot.subscribe(new TelegramGroupMessageReactor(update -> {
            try {
                sendMessageFromTlgGroupChat(update.getMessage());
                return true;
            }catch (Exception e){
                return false;
            }
        }));

        mainBot.subscribe(new TelegramGroupEditMessageReactor(update -> {
            try {
                sendMessageFromTlgGroupChat(update.getMessage());
                return true;
            }catch (Exception e){
                return false;
            }
        }));
    }

    private boolean sendWorkLogQueue(Long chatId) throws TelegramApiException {
        List<WorkLog> notAcceptedWorkLogs = new ArrayList<>();
        AtomicBoolean hasActive = new AtomicBoolean(false);
        List<WorkLog> queueByTelegramId = workLogDispatcher.getQueueByTelegramId(chatId);
        for (WorkLog workLog : queueByTelegramId) {
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
                PhotoSize bigPhoto = message.getPhoto().stream().max(comparingInt(a -> (a.getWidth() + a.getHeight()))).orElse(null);
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
        this.telegramConf = telegramConf;
        stompController.changeTelegramConfig(telegramConf);
        initializeMainBot();
    }

    public void sendNotification(Employee employee, Notification notification) {
        if (mainBot == null) {
            log.warn("Попытка отправить уведомление при не инициализированном TelegramApi");
            return;
        }
        if (employee.getTelegramUserId() == null || employee.getTelegramUserId().isBlank() || notification.getMessage() == null) return;
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

//    private void broadcastEditChatMessage(ChatDto chat, ChatMessageDto chatMessage) {
//
//    }

//    public void refreshChatUnreadCount(Chat chat, SuperMessage superMessage) {
//        chat.getMembers()
//                .stream()
//                .filter(e -> !e.getLogin().equals(superMessage.getAuthor().getLogin()))
//                .forEach(e -> {
//                    String login = e.getLogin();
//                    Long chatId = chat.getChatId();
//                    Long countUnreadMessages = chatDispatcher.getUnreadMessagesCount(chatId, e);
//                    stompController.updateCountUnreadMessage(login, chatId, countUnreadMessages);
//                });
//    }

//    /**
//     * Обрабатывает сообщения полученные из web версии crm, и отправляет их всем пользователям чата
//     *
//     * @param chatId      Идентификатор чата из базы данных
//     * @param messageData Данные сообщения полученные из web
//     * @param author      Объект сотрудника отправившего сообщение
//     * @throws EntryNotFound        Если не находит чат или сообщение в базе данных
//     * @throws EmptyFile            Попытка сохранить пустой файл
//     * @throws WriteError           Ошибка при записи файла на диск
//     * @throws TelegramApiException Ошибка при обращении к telegram api
//     * @throws IllegalFields        Ошибка при попытке отправить мультимедиа-сообщение без вложения
//     * @throws IllegalMediaType     Ошибка при неудачной попытке определить тип данных вложения
//     */
//    public void sendMessageFromWeb(Long chatId, MessageData messageData, Employee author) throws EntryNotFound,
//            EmptyFile, WriteError, TelegramApiException, IllegalFields, IllegalMediaType {
//        // Находим целевой чат
//        Chat chat = chatDispatcher.getChat(chatId);
//
//        if (chat.getClosed() != null) {
//            throw new IllegalFields("Не возможно отправить сообщение в закрытый чат");
//        }
//
//        if (!chat.getMembers().contains(author)) {
//            chat.getMembers().add(author);
//            List<SuperMessage> superMessages = chatDispatcher.setAllMessagesAsRead(chat.getChatId(), author);
//            superMessages.forEach(stompController::updateMessage);
//            stompController.updateChat(chatDispatcher.unsafeSave(chat));
//            stompController.updateCountUnreadMessage(author.getLogin(), chatId, 0L);
//        }
//
//        // Из messageData создаем одно или несколько сообщений в зависимости от вложений
//        Set<Attachment> attachments = new HashSet<>();
//        if (!messageData.getFiles().isEmpty()) {
//            attachments = new HashSet<>(attachmentDispatcher.saveAttachments(messageData.getFiles()));
//        }
//
//        if (attachments.size() > 1) {
//            // Если вложений больше 1 создаем несколько сообщений по медиа группам для корректной отправки в tg
//
//
//            List<Attachment> visualMedia = attachments.stream()
//                    .filter(a -> a.getType() == AttachmentType.PHOTO || a.getType() == AttachmentType.VIDEO)
//                    .collect(Collectors.toList());
//            List<Attachment> audioMedia = attachments.stream()
//                    .filter(a -> a.getType() == AttachmentType.AUDIO)
//                    .collect(Collectors.toList());
//            List<Attachment> documentsMedia = attachments.stream()
//                    .filter(a -> a.getType() == AttachmentType.DOCUMENT || a.getType() == AttachmentType.FILE)
//                    .collect(Collectors.toList());
//
//            if (messageData.getText() != null && !messageData.getText().isBlank()) {
//                // Создаем текстовое сообщение, если есть текст
//
//                // Записываем в бд и транслируем в tg чаты членов чата
//                SuperMessage textMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, messageData.getReplyMessageId(), this);
//
//                // Обновляем web
//                broadcastUpdatesToWeb(textMessage);
//            } else {
//                // Если текста нет, транслируем в чаты метку отправителя
//
//                sendTextBroadcastMessage(chat, ChatMessage.of("", author));
//            }
//
//            // Создаем, отправляем и транслируем медиа сообщения в зависимости от количества вложений
//            if (visualMedia.size() == 1) {
//                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, visualMedia.get(0), messageData.getReplyMessageId(), this);
//
//                // Отправляем обновления в интерфейс пользователя
//                broadcastUpdatesToWeb(mediaMessage);
//            } else if (visualMedia.size() > 1) {
//                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, visualMedia, messageData.getReplyMessageId(), this);
//
//                // Отправляем обновления в интерфейс пользователя
//                broadcastUpdatesToWeb(mediaMessage);
//            }
//            if (audioMedia.size() == 1) {
//                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, audioMedia.get(0), messageData.getReplyMessageId(), this);
//
//                // Отправляем обновления в интерфейс пользователя
//                broadcastUpdatesToWeb(mediaMessage);
//            } else if (audioMedia.size() > 1) {
//                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, audioMedia, messageData.getReplyMessageId(), this);
//
//                // Отправляем обновления в интерфейс пользователя
//                broadcastUpdatesToWeb(mediaMessage);
//            }
//            if (documentsMedia.size() == 1) {
//                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, documentsMedia.get(0), messageData.getReplyMessageId(), this);
//
//                // Отправляем обновления в интерфейс пользователя
//                broadcastUpdatesToWeb(mediaMessage);
//            } else if (documentsMedia.size() > 1) {
//                SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, documentsMedia, messageData.getReplyMessageId(), this);
//
//                // Отправляем обновления в интерфейс пользователя
//                broadcastUpdatesToWeb(mediaMessage);
//            }
//
//        } else if (attachments.size() == 1) {
//            // Если вложение одно, то создаем простое медиа сообщение
//
//            Attachment attachment = attachments.iterator().next();
//
//            SuperMessage mediaMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, attachment, messageData.getReplyMessageId(), this);
//
//            // Отправляем обновления в интерфейс пользователя
//            broadcastUpdatesToWeb(mediaMessage);
//        } else {
//            // Если вложений нет, то создаем текстовое сообщение.
//
//            // Записываем в бд и транслируем в tg чаты членов чата
//            SuperMessage textMessage = chatDispatcher.createMessage(chatId, messageData.getText(), author, messageData.getReplyMessageId(), this);
//
//            // Обновляем web
//            broadcastUpdatesToWeb(textMessage);
//        }
//    }

//    /**
//     * Удаляет сообщение из базы данных и из telegram api при запросе от web crm
//     *
//     * @param messageId Идентификатор целевого сообщения
//     * @param employee  Кто удаляет сообщение
//     * @return Список удаленных сообщений
//     * @throws EntryNotFound  Если сообщение не найдено
//     * @throws NotOwner       Если сообщение не принадлежит сотруднику
//     * @throws AlreadyDeleted Если сообщение уже удалено
//     */
//    public SuperMessage deleteMessageFromWeb(Long messageId, Employee employee) throws EntryNotFound, NotOwner, AlreadyDeleted, TelegramApiException {
//        List<ChatMessage> listOfRelatedMessages = chatDispatcher.getListOfRelatedMessages(messageId);
//        ChatMessageMediaGroup listOfDeletedMessages = new ChatMessageMediaGroup();
//        for (ChatMessage chatMessage : listOfRelatedMessages) {
//            listOfDeletedMessages.addMessage(chatDispatcher.deleteMessage(chatMessage.getChatMessageId(), employee));
//            deleteMessageFromTelegram(chatMessage);
//        }
//        SuperMessage superMessage = listOfDeletedMessages.getSuperMessage();
//        stompController.deleteMessage(superMessage);
//        return superMessage;
//    }


//    /**
//     * Редактирует сообщение из базы данных и из telegram api при запросе от web crm
//     *
//     * @param editMessageId Идентификатор редактируемого сообщения
//     * @param text          Отредактированный текст
//     * @param author        Кто редактирует сообщение
//     * @throws TelegramApiException Если ошибка при отправке сообщения в telegram api
//     * @throws EntryNotFound        Если сообщение не найдено
//     * @throws NotOwner             Если сообщение не принадлежит сотруднику
//     * @throws IllegalFields        Если сообщение пустое или изменений нет
//     */
//    public void updateMessageFromWeb(Long editMessageId, String text, Employee author) throws TelegramApiException, EntryNotFound, NotOwner, IllegalFields {
//        stompController.updateMessage(chatMessageDispatcher.updateMessageFromWeb(editMessageId, text, author, this));
//    }

    private void deleteMessageFromTelegram(ChatMessage chatMessage) throws TelegramApiException {
        for (TelegramMessageBind telegramMessageBind : chatMessage.getTelegramBinds()) {
            new TelegramMessageFactory(telegramMessageBind.getTelegramChatId().toString(), mainBot).deleteMessage(telegramMessageBind.getTelegramMessageId()).execute();
        }
    }

//    public void sendMessageFromTlgChat(Message receivedMessage) throws EntryNotFound, TelegramApiException, IllegalFields, SaveEntryFailed, IllegalMediaType, ExceptionInsideThread {
//        // Получаем автора сообщения
//        Employee author = employeeDispatcher.getByTelegramId(receivedMessage.getChatId()).orElseThrow(() -> new EntryNotFound("Идентификатор телеграм не привязан ни к одному аккаунту"));
//        // Пытаемся получить активный журнал задачи монтажника который написал сообщение,
//        // чтобы понять в какой чат транслировать сообщение.
//        WorkLog activeWorkLog = workLogDispatcher.getAcceptedByTelegramIdDTO(receivedMessage.getChatId());
//        // Получаем целевой чат
//        Chat chat = activeWorkLog.getChat();
//        if (chat == null) throw new IllegalFields("Чат не прикреплен к журналу работ");
//
//        switch (Utils.getTlgMsgType(receivedMessage)) {
//            case TEXT, MEDIA -> {
//                // Создает сообщение в базе данных
//                SuperMessage textMessage = chatDispatcher.createMessage(chat.getChatId(), receivedMessage, author, this, false);
//                broadcastUpdatesToWeb(textMessage);
//            }
//            case GROUP -> {
//                String group = Utils.getTlgMsgGroupId(receivedMessage);
//                if (groupedMessagesFromTelegram.containsKey(group)) {
//                    groupedMessagesFromTelegram.get(group).add(receivedMessage);
//                } else {
//                    groupedMessagesFromTelegram.put(group, Stream.of(receivedMessage).collect(Collectors.toList()));
//                    try {
//                        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
//                        threadExecutor.execute(() -> {
//                            try {
//                                // Ждем несколько секунд пока придут все сообщения от telegram api
//                                sleep(1000);
//                                List<Message> listOfReceivedMessages = groupedMessagesFromTelegram.get(group);
//                                SuperMessage mediaGroupMessage = chatDispatcher.createMessage(chat.getChatId(), listOfReceivedMessages, author, this, false);
//                                // Отправляет сообщение для обновления пользовательского интерфейса
//                                broadcastUpdatesToWeb(mediaGroupMessage);
//                            } catch (InterruptedException | TelegramApiException | EntryNotFound |
//                                     IllegalFields | IllegalMediaType e) {
//                                throw new RuntimeException(e);
//                            } finally {
//                                groupedMessagesFromTelegram.remove(group);
//                            }
//                        });
//                        threadExecutor.shutdown();
//                    } catch (RuntimeException e) {
//                        throw new ExceptionInsideThread(e.getMessage());
//                    }
//                }
//            }
//        }
//    }

    public void sendMessageFromTlgGroupChat(Message receivedMessage) throws EntryNotFound, TelegramApiException, IllegalFields, SaveEntryFailed, IllegalMediaType, ExceptionInsideThread {

        // Получаем автора сообщения
        Employee author = employeeDispatcher.getByTelegramId(receivedMessage.getFrom().getId()).orElseThrow(() -> new EntryNotFound("Идентификатор телеграм не привязан ни к одному аккаунту"));

        // Получаем монтажников в группе
        List<Employee> employeesByGroup = employeeDispatcher.getByGroupTelegramId(receivedMessage.getChatId());

        Set<Chat> chats = new HashSet<>();
        for (Employee employee : employeesByGroup) {
            chats.add(workLogDispatcher.getActiveChatByEmployee(employee));
        }

//        for (Chat chat : chats){
//            if (!chat.getMembers().contains(author)) {
//                chat.getMembers().add(author);
//                List<SuperMessage> superMessages = chatDispatcher.setAllMessagesAsRead(chat.getChatId(), author);
//                superMessages.forEach(stompController::updateMessage);
//                stompController.updateChat(chatDispatcher.unsafeSave(chat));
//                stompController.updateCountUnreadMessage(author.getLogin(), chat.getChatId(), 0L);
//            }
//        }

        switch (Utils.getTlgMsgType(receivedMessage)) {
            case TEXT, MEDIA -> {
                for (Chat chat : chats){
                    // Создает сообщение в базе данных
                    SuperMessage textMessage = chatDispatcher.createMessage(chat.getChatId(), receivedMessage, author, this);
                    broadcastUpdatesToWeb(textMessage);
                }
            }
            case GROUP -> {
                String group = Utils.getTlgMsgGroupId(receivedMessage);
                if (groupedMessagesFromTelegram.containsKey(group)) {
                    groupedMessagesFromTelegram.get(group).add(receivedMessage);
                } else {
                    groupedMessagesFromTelegram.put(group, Stream.of(receivedMessage).collect(Collectors.toList()));
                    try {
                        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
                        threadExecutor.execute(() -> {
                            try {
                                // Ждем несколько секунд пока придут все сообщения от telegram api
                                sleep(1000);
                                List<Message> listOfReceivedMessages = groupedMessagesFromTelegram.get(group);
                                for (Chat chat : chats) {
                                    SuperMessage mediaGroupMessage = chatDispatcher.createMessage(chat.getChatId(), listOfReceivedMessages, author, this);
                                    // Отправляет сообщение для обновления пользовательского интерфейса
                                    broadcastUpdatesToWeb(mediaGroupMessage);
                                }
                            } catch (InterruptedException | TelegramApiException | EntryNotFound |
                                     IllegalFields | IllegalMediaType e) {
                                throw new RuntimeException(e);
                            } finally {
                                groupedMessagesFromTelegram.remove(group);
                            }
                        });
                        threadExecutor.shutdown();
                    } catch (RuntimeException e) {
                        throw new ExceptionInsideThread(e.getMessage());
                    }
                }
            }
        }
    }

    public void broadcastUpdatesToWeb(SuperMessage message) throws EntryNotFound {
        if (message != null) {
            ChatDto chat = chatDispatcher.getChatDto(message.getParentChatId());
            stompController.createMessage(message, chat.getMembers());
            stompController.updateChat(ChatMapper.fromDto(chat));
//            refreshChatUnreadCount(ChatMapper.fromDto(chat), message);
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

//    /**
//     * Отправляет сообщения броадкастом во все телеграм чаты
//     *
//     * @param chat    {@link Chat} из базы данных
//     * @param message Отправляемое текстовое сообщение
//     * @return Список ответов от telegram api об отправленных сообщениях
//     * @throws TelegramApiException Если возникла ошибка при отправке сообщения
//     */
//    public List<Message> sendTextBroadcastMessage(Chat chat, ChatMessageDto message) throws TelegramApiException {
//        // Список ответов от telegram об отправленных сообщениях
//        List<Message> responses = new ArrayList<>();
//
//        Set<String> memberGroups = chat.getMembers().stream().map(Employee::getTelegramGroupChatId).filter(Objects::nonNull).filter(s->!s.isBlank()).collect(Collectors.toSet());
//
//        Set<Employee> membersWithoutGroup = chat.getMembers().stream().filter(Employee::isHasNotGroup).collect(Collectors.toSet());
//
//
//        for (String group : memberGroups) {
//            responses.add(TelegramMessageFactory.create(group, mainBot).broadcastTextMessage(message).execute());
//        }
//
//        // Проходимся по всем членам чата, проверяем привязан ли телеграм, и не является ли целевой чат автором сообщения
//        for (Employee employee : membersWithoutGroup) {
//            // Идентификатор чата в телеграм
//            String targetChatId = employee.getTelegramUserId();
//
//            // Проверка валидности чата
//            if (targetChatId == null || targetChatId.isBlank() || employee.getLogin().equals(message.getAuthor().getLogin()))
//                continue;
//            try {
//                responses.add(TelegramMessageFactory.create(targetChatId, mainBot).broadcastTextMessage(message).execute());
//            }catch (Throwable e){
//                log.warn("Не удалось отправить сообщение в чат {} {}", targetChatId, employee.getLogin());
//            }
//        }
//
//        return responses;
//    }

    public void sendMessageToTlgId(String chatId, String message) throws TelegramApiException {
        TelegramMessageFactory.create(chatId, mainBot).simpleMessage(message).execute();
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

        Set<String> memberGroups = chat.getMembers().stream().map(Employee::getTelegramGroupChatId).filter(Objects::nonNull).filter(s->!s.isBlank()).collect(Collectors.toSet());

        Set<Employee> membersWithoutGroup = chat.getMembers().stream().filter(Employee::isHasNotGroup).collect(Collectors.toSet());

        for (String group : memberGroups) {
            responses.add(TelegramMessageFactory.create(group, mainBot).broadcastMediaMessage(message).execute());
        }

        // Проходимся по всем членам чата, проверяем привязан ли телеграм, и не является ли целевой чат автором сообщения
        for (Employee employee : membersWithoutGroup) {
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

        Set<String> memberGroups = chat.getMembers().stream().map(Employee::getTelegramGroupChatId).filter(Objects::nonNull).filter(s->!s.isBlank()).collect(Collectors.toSet());

        Set<Employee> membersWithoutGroup = chat.getMembers().stream().filter(Employee::isHasNotGroup).collect(Collectors.toSet());

        for (String group : memberGroups) {
            responses.add(TelegramMessageFactory.create(group, mainBot).broadcastMediaGroupMessage(messages).execute());
        }

        for (Employee employee : membersWithoutGroup) {
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

    public void assignInstallers(WorkLog workLog, Employee employee, List<Employee> acceptedEmployees) throws TelegramApiException {
        if(workLog.getGangLeader() != null){
            Employee gangLeader = workLog.getEmployees().stream().filter(emp -> Objects.equals(emp.getLogin(), workLog.getGangLeader())).findFirst().orElseThrow(() -> {
                workLogDispatcher.remove(workLog);
                return new ResponseException("Бригадира нет в списке монтажников");
            });
            boolean hasNotTelegram = workLog.getEmployees().stream().anyMatch(ins -> ins.getTelegramUserId() == null || ins.getTelegramUserId().isBlank());
            if(hasNotTelegram){
                workLogDispatcher.remove(workLog);
                throw new ResponseException("У бригадира не назначен telegram id");
            }
            try {
                TelegramMessageFactory messageFactory = TelegramMessageFactory.create(gangLeader.getTelegramUserId(), mainBot);
                if(acceptedEmployees.contains(gangLeader)){
                    Task task = workLog.getTask();
                    messageFactory.simpleMessage("Вам назначена еще одна задача:\n"+ task.getClassName() + " - " + task.getTypeName()).execute();
                }else{
                    messageFactory.acceptWorkLog(workLog, employee).execute();
                }
            }catch (Throwable e){
                workLogDispatcher.remove(workLog);
                throw new ResponseException("Бригадир имеет не верный telegram id");
            }
            return;
        }
        boolean hasNotTelegram = workLog.getEmployees().stream().anyMatch(ins -> ins.getTelegramUserId() == null || ins.getTelegramUserId().isBlank());
        if (hasNotTelegram){
            workLogDispatcher.remove(workLog);
            throw new ResponseException("У сотрудников не назначен telegram id");
        }
        try {
            for (Employee installer : workLog.getEmployees()) {
                TelegramMessageFactory messageFactory = TelegramMessageFactory.create(installer.getTelegramUserId(), mainBot);
                if(acceptedEmployees.contains(installer)){
                    Task task = workLog.getTask();
                    messageFactory.simpleMessage("Вам назначена еще одна задача:\n"+ task.getClassName() + " - " + task.getTypeName()).execute();
                }else{
                    messageFactory.acceptWorkLog(workLog, employee).execute();
                }
            }
        }catch (Throwable e){
            workLogDispatcher.remove(workLog);
            throw new ResponseException("Один или несколько сотрудников имеют не верный telegram id");
        }
    }

    public void sendDhcpIpRequestNotification(DhcpIpRequestNotificationBody body) throws TelegramApiException {
        if(telegramConf == null || !telegramConf.isFilled()) {
            log.warn("Отсутствует конфигурация телеграмма");
            return;
        }
        String chatId = telegramConf.getDhcpNotificationChatId();
        if(chatId == null || chatId.isBlank()) {
            log.warn("Чат для отправки DhcpIpRequestNotification отсутствует");
            return;
        }
        TelegramMessageFactory.create(chatId, mainBot).dhcpIpRequestNotification(body).execute();
    }
    
    public Employee getEmployeeByChat(Long chatId) throws Exception {
        return employeeDispatcher.getByTelegramId(chatId).orElseThrow(()->new Exception("Пользователь не найден"));
    }

    public TelegramConf getConfiguration() {
        if(telegramConf == null){
            return new TelegramConf();
        }
        return telegramConf;
    }
    
    public enum OperatingMode{
        REPORT_SENDING("REPORT_SENDING"),
        CHECK_ALIVE("CHECK_ALIVE"),
        HOUSE_SESSIONS("HOUSE_SESSIONS"),
        SEARCH_FILES("SEARCH_FILES"),
        COMMUTATOR_SESSIONS("COMMUTATOR_SESSIONS");
        
        private final String value;
        
        OperatingMode(String value){
            this.value = value;
        }
        
        public String getValue(){
            return value;
        }
    }
}
