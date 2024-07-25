package com.microel.trackerbackend.controllers.telegram;

import com.microel.tdo.EventType;
import com.microel.tdo.UpdateCarrier;
import com.microel.tdo.network.NetworkMediaGroup;
import com.microel.tdo.network.NetworkSendPhoto;
import com.microel.trackerbackend.controllers.configuration.Configuration;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.AutoSupportNodes;
import com.microel.trackerbackend.controllers.configuration.entity.UserTelegramConf;
import com.microel.trackerbackend.controllers.telegram.reactor.*;
import com.microel.trackerbackend.misc.Async;
import com.microel.trackerbackend.misc.autosupport.AutoSupportContext;
import com.microel.trackerbackend.misc.autosupport.SendingNodeException;
import com.microel.trackerbackend.misc.autosupport.schema.AutoSupportSession;
import com.microel.trackerbackend.misc.autosupport.schema.AutoSupportStorage;
import com.microel.trackerbackend.modules.transport.Credentials;
import com.microel.trackerbackend.services.UserAccountService;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.dispatchers.ReviewDispatcher;
import com.microel.trackerbackend.storage.dispatchers.UserRequestDispatcher;
import com.microel.trackerbackend.storage.entities.users.Review;
import com.microel.trackerbackend.storage.entities.users.TelegramUserAuth;
import com.microel.trackerbackend.storage.entities.users.UserRequest;
import com.microel.trackerbackend.storage.entities.users.UserTariff;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.UserTariffRepository;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.http.RequestEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@Service
public class UserTelegramController {
    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final Configuration configuration;
    private final StompController stompController;
    private final UserAccountService userAccountService;
    private final String authSalt = "3xv23sj*NYDgNHk43m(Z1P6P4:PW?s/i";
    private final ApiBillingController apiBillingController;
    private final UserTariffRepository userTariffRepository;
    private final Map<Long, Mode> usersInMode = new ConcurrentHashMap<>();
    private final Map<Long, AutoSupportSession> autoSupportSessions = new ConcurrentHashMap<>();
    private final ReviewDispatcher reviewDispatcher;
    private final AutoSupportContext autoSupportContext;
    private final UserRequestDispatcher userRequestDispatcher;
    private UserTelegramConf telegramConf;
    private AutoSupportNodes autoSupportNodes;
    @Nullable
    private TelegramBotsApi api;
    @Nullable
    private MainBot mainBot;
    @Nullable
    private BotSession mainBotSession;

    public UserTelegramController(Configuration configuration, StompController stompController, UserAccountService userAccountService, ApiBillingController apiBillingController, UserTariffRepository userTariffRepository, ReviewDispatcher reviewDispatcher, AutoSupportContext autoSupportContext, UserRequestDispatcher userRequestDispatcher) {
        this.configuration = configuration;
        this.stompController = stompController;
        this.userAccountService = userAccountService;
        this.reviewDispatcher = reviewDispatcher;
        this.autoSupportContext = autoSupportContext;
        this.userRequestDispatcher = userRequestDispatcher;
        try {
            this.autoSupportNodes = configuration.load(AutoSupportNodes.class);
        } catch (Exception e) {
            System.out.println("Конфигурация для Telegram не найдена");
        }
        this.apiBillingController = apiBillingController;
        this.userTariffRepository = userTariffRepository;
        initialize();
    }

    private String hashWithSalt(Long input) {
        String saltedInput = input + authSalt;
        byte[] hash = DigestUtils.sha256(saltedInput);
        return Hex.encodeHexString(hash);
    }

    private void initializeApi() throws TelegramApiException {
        if (api == null) api = new TelegramBotsApi(DefaultBotSession.class);
    }

    public void initialize() {
        Async.of(() -> {
            try {
                telegramConf = configuration.load(UserTelegramConf.class);
                initializeMainBot();
            } catch (FailedToReadConfigurationException e) {
                System.out.println("Конфигурация для Telegram не найдена");
            } catch (TelegramApiException e) {
                System.out.println("Ошибка Telegram API " + e.getMessage());
                try {
                    sleep(5000);
                    initialize();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
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

        List<BotCommand> commands = List.of(new BotCommand("home", "Главное меню"));
        SetMyCommands setMyCommands = SetMyCommands.builder().commands(commands).build();
        mainBot.execute(setMyCommands);


        mainBot.subscribe(new TelegramChatJoinReactor(update -> {
            System.out.println(update);
            return true;
        }));

        mainBot.subscribe(new TelegramCommandReactor("/start", update -> {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            TelegramMessageFactory.create(chatId, mainBot).linkForUserAuth(chatId, hashWithSalt(chatId)).execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCommandReactor("/home", update -> {
            Message message = update.getMessage();
            if (!isAuth(message.getChatId())) return false;
            TelegramMessageFactory.create(message.getChatId(), mainBot).userMainMenu("Главное меню").execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("suspend_confirmed", (update, callbackData) -> {
            if (callbackData == null) return false;
            final Long chatId = update.getCallbackQuery().getFrom().getId();
            final Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            factory.editTextOfMessage(messageId, "Приостановка обслуживания...").execute();
//            Message tempMsg = factory.simpleMessage("Приостановка обслуживания...").execute();
            factory.userMainMenu("Главное меню").execute();
            try {
                apiBillingController.stopUserService(callbackData.getString());
                factory.editTextOfMessage(messageId, "Приостановка обслуживания прошла успешно.").execute();
            } catch (Exception e) {
                factory.editTextOfMessage(messageId, "Не удалось приостановить обслуживание.").execute();
                return false;
            }
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("suspend_not_confirmed", (update, callbackData) -> {
            if (callbackData == null) return false;
            final Long chatId = update.getCallbackQuery().getFrom().getId();
            final Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            factory.deleteMessage(messageId).execute();
            factory.userMainMenu("Главное меню").execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("connect_user_service", (update, callbackData) -> createUserRequest(update, callbackData, "Подключение услуги", UserRequest.Type.APPEND_SERVICE)));

        mainBot.subscribe(new TelegramCallbackReactor("disconnect_user_service", (update, callbackData) -> createUserRequest(update, callbackData, "Удаление услуги", UserRequest.Type.REMOVE_SERVICE)));

        mainBot.subscribe(new TelegramCallbackReactor("change_user_tariff", (update, callbackData) -> createUserRequest(update, callbackData, "Изменение тарифа", UserRequest.Type.REPLACE_TARIFF)));

        mainBot.subscribe(new TelegramCallbackReactor("get_user_requests", (update, callbackData) -> {
            if (callbackData == null) return false;
            final Long chatId = update.getCallbackQuery().getFrom().getId();
            final Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            final TelegramUserAuth userAccount = userAccountService.getUserAccount(chatId);
            TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            if (userAccount == null) {
                factory.linkForUserAuth(chatId, hashWithSalt(chatId)).execute();
                return false;
            }
            // Ищем нереализованные запросы
            List<UserRequest> existedRequests = userRequestDispatcher.getUnprocessedRequestsByLogin(userAccount.getUserLogin());
            factory.deleteMessage(messageId).execute();
            factory.listOfUserRequests(existedRequests).execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("remove_user_request", (update, callbackData) -> {
            if (callbackData == null || callbackData.getLong() == null) return false;
            final Long chatId = update.getCallbackQuery().getFrom().getId();
            final TelegramUserAuth userAccount = userAccountService.getUserAccount(chatId);
            TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            if (userAccount == null) {
                factory.linkForUserAuth(chatId, hashWithSalt(chatId)).execute();
                return false;
            }
            try {
                userRequestDispatcher.cancelRequest(callbackData.getLong());
            } catch (EntryNotFound e) {
                throw new TelegramApiException(e);
            }
            factory.answerCallback(update.getCallbackQuery().getId(), "Запрос успешно отменен").execute();
            factory.removeInlineButton(update.getCallbackQuery().getMessage(), callbackData).execute();
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("as", (update, callbackData) -> {
            if (callbackData == null || callbackData.getUUID() == null) return false;
            final Long chatId = update.getCallbackQuery().getFrom().getId();
            final Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            AutoSupportSession autoSupportSession = autoSupportSessions.get(chatId);
            if (autoSupportSession == null) {
                TelegramMessageFactory.create(chatId, mainBot).answerCallback(update.getCallbackQuery().getId(), "Ошибка сервера").execute();
                return false;
            }
            autoSupportSession.callbackUpdate(chatId, callbackData, () -> {
                final TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
                try {
                    factory.clearInlineKeyboardMenu(messageId).execute();
                    factory.answerCallback(update.getCallbackQuery().getId(), null).execute();
                } catch (TelegramApiException e) {
                    System.out.println(e.getMessage());
                }
            });
            return true;
        }));

        mainBot.subscribe(new TelegramCallbackReactor("as_no_phone", (update, callbackData) -> {
            final Long chatId = update.getCallbackQuery().getFrom().getId();
            final Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            final TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
            AutoSupportSession autoSupportSession = autoSupportSessions.get(chatId);
            if (autoSupportSession == null) {
                factory.answerCallback(update.getCallbackQuery().getId(), "Ошибка сервера").execute();
                return false;
            }
            autoSupportSession.phoneNumberRejected(chatId);
            factory.clearInlineKeyboardMenu(messageId).execute();
            return true;
        }));

        mainBot.subscribe(new TelegramMessageReactor(update -> {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            TelegramUserAuth userAccount = userAccountService.getUserAccount(chatId);
            if (userAccount == null) {
                TelegramMessageFactory.create(chatId, mainBot).linkForUserAuth(chatId, hashWithSalt(chatId)).execute();
                return false;
            }
            try {
                if (handleMainMenuCommand(message, userAccount)) {
                    return true;
                }
                Mode userMode = getUserMode(userAccount.getUserId());
                if (userMode != null) {
                    switch (userMode) {
                        case REVIEW -> {
                            return handleSendReview(message, userAccount);
                        }
                        case AUTO_SUPPORT -> {
                            AutoSupportSession session = autoSupportSessions.get(userAccount.getUserId());
                            if (session != null) {
                                session.textUpdate(update, userAccount.getUserId());
                                return true;
                            } else {
                                clearUserMode(userAccount.getUserId());
                                return false;
                            }
                        }
                    }
                }
                sendUpdateToHub(update);
            } catch (Exception e) {
                throw new TelegramApiException(e.getMessage());
            }
            return true;
        }));

        mainBot.subscribe(new TelegramEditMessageReactor(update -> {
            sendUpdateToHub(update);
            return true;
        }));
    }

    private boolean createUserRequest(Update update, @Nullable CallbackData callbackData, String title, UserRequest.Type type) throws TelegramApiException {
        if (callbackData == null) return false;
        final Long chatId = update.getCallbackQuery().getFrom().getId();
        final Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        final TelegramUserAuth userAccount = userAccountService.getUserAccount(chatId);
        TelegramMessageFactory factory = TelegramMessageFactory.create(chatId, mainBot);
        if (userAccount == null) {
            factory.linkForUserAuth(chatId, hashWithSalt(chatId)).execute();
            return false;
        }
        UserTariff userTariff = userTariffRepository.findAll((root, query, cb) -> cb.and(cb.equal(root.get("baseName"),
                callbackData.getString()), cb.isFalse(root.get("deleted")))).stream().findFirst().orElseThrow(() -> new TelegramApiException("Произошла ошибка"));

        String requestDescription = title + " " + userTariff.getName();
        try {
            UserRequest userRequest = userRequestDispatcher.createRequest(userAccount.getUserLogin(), type, title, requestDescription,
                    UserRequest.Source.TELEGRAM, chatId.toString(), null);
            factory.userRequestResponse(messageId, "Успешно создан запрос - " + userRequest.getDescription()).execute();
            return true;
        } catch (AlreadyExists e) {
            factory.userRequestResponse(messageId, "Данный запрос уже существует").execute();
            return false;
        }
    }

    public void sendUpdateToHub(Update update) throws TelegramApiException {
        try {
            RequestEntity.BodyBuilder request = RequestEntity.post(url("updates"));
            restTemplate.exchange(request.body(update), Void.class);
        } catch (Exception e) {
            throw new TelegramApiException("Не удалось отправить сообщение, повторите попытку позже");
        }
    }

    public void sendReviewNotification(Review review) throws TelegramApiException {
        if (telegramConf == null || !telegramConf.isFilled()) {
            System.out.println("Отсутствует конфигурация телеграмма");
            return;
        }
        String chatId = telegramConf.getNotificationChannelId();
        if (chatId == null || chatId.isBlank()) {
            System.out.println("Чат для отправки DhcpIpRequestNotification отсутствует");
            return;
        }
        TelegramMessageFactory.create(chatId, mainBot).reviewNotification(review).execute();
    }

    public UUID createNewChat(Long chatId, String chatName) {
        RequestEntity<Void> request = RequestEntity.get(url("new-chat/" + chatId + "?chatName=" + chatName)).build();
        return restTemplate.exchange(request, UUID.class).getBody();
    }

    private String url(String... params) {
        return "http://" + telegramConf.getMicroelHubIpPort() + "/api/public/telegram/" + String.join("/", params);
    }

    public UserTelegramConf getConfiguration() {
        if (telegramConf == null) {
            return new UserTelegramConf();
        }
        return telegramConf;
    }

    public void changeTelegramConf(UserTelegramConf telegramConf) throws FailedToWriteConfigurationException, TelegramApiException, IOException {
        configuration.save(telegramConf);
        this.telegramConf = telegramConf;
        stompController.changeUserTelegramConfig(telegramConf);
        initializeMainBot();
    }

    public void changeAutoSupportNodes(AutoSupportNodes nodes) {
        configuration.save(nodes);
        this.autoSupportNodes = nodes;
    }

    public Boolean checkSecret(UserTelegramCredentials credentials) {
        return hashWithSalt(credentials.getId()).equals(credentials.getSc());
    }

    public boolean doSendAuthConfirmation(Long id, ApiBillingController.TotalUserInfo userInfo) {
        try {
            String fullName = userInfo.getIbase().getFio();
            String greeting = "Здравствуйте, " + fullName + "\nВы успешно авторизованы \uD83D\uDC4D";
            TelegramMessageFactory.create(id, mainBot).userMainMenu(greeting).execute();
        } catch (TelegramApiException e) {
            return false;
        }
        return true;
    }

    public boolean isAuth(Long userId) throws TelegramApiException {
        TelegramUserAuth userAccount = userAccountService.getUserAccount(userId);
        if (userAccount != null) return true;
        TelegramMessageFactory.create(userId, mainBot).linkForUserAuth(userId, hashWithSalt(userId)).execute();
        return false;
    }

    @Nullable
    private String convertToCommand(Message message) {
        String text = message.getText();
        if (text == null) return null;
        Pattern pattern = Pattern.compile("([А-я .]+)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        return matcher.group(1).strip();
    }

    private boolean handleMainMenuCommand(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        String command = convertToCommand(message);
        if (command == null) return false;
        switch (command) {
            case UserMenuCommands.MAIN_MENU, UserMenuCommands.CANCEL -> {
                return handleShowMainMenu(message, userAccount);
            }
            case UserMenuCommands.SHOW_TARIFF -> {
                return handleTariffRequest(message, userAccount);
            }
            case UserMenuCommands.SHOW_SERVICES -> {
                return handleServicesRequest(message, userAccount);
            }
            case UserMenuCommands.SHOW_BALANCE -> {
                return handleBalanceRequest(message, userAccount);
            }
            case UserMenuCommands.SUSPENSION_OF_SERVICE -> {
                return handleSuspendRequest(message, userAccount);
            }
            case UserMenuCommands.CONNECT_SERVICE -> {
                return handleConnectService(message, userAccount);
            }
            case UserMenuCommands.DISCONNECT_SERVICE -> {
                return handleDisconnectService(message, userAccount);
            }
            case UserMenuCommands.CHANGE_TARIFF -> {
                return handleChangeTariffRequest(message, userAccount);
            }
            case UserMenuCommands.REMOVE_REQUESTS -> {
                return handleListRemoveRequests(message, userAccount);
            }
            case UserMenuCommands.RESUME_SERVICE -> {
                return handleResumeService(message, userAccount);
            }
            case UserMenuCommands.ENABLE_DEFERRED_PAYMENT -> {
                return handleEnableDeferredPayment(message, userAccount);
            }
            case UserMenuCommands.CREATE_REVIEW -> {
                return handleCreateReview(message, userAccount);
            }
            case UserMenuCommands.I_HAVE_A_PROBLEM -> {
                return handleIHaveAProblem(message, userAccount);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleIHaveAProblem(Message message, TelegramUserAuth userAccount) {
        TelegramMessageFactory factory = TelegramMessageFactory.create(message.getChatId(), mainBot);
        AutoSupportSession autoSupportSession = new AutoSupportSession(autoSupportContext, new AutoSupportStorage(), autoSupportNodes.getDefaultNodes(), factory);
        setAutoSupportSession(userAccount.getUserId(), autoSupportSession);
        setUserMode(userAccount.getUserId(), Mode.AUTO_SUPPORT);
        try {
            factory.cancelReplyMenu("Вы находитесь в меню 'Автоматический помощник'. Следуйте дальнейшим инструкциям или нажмите кнопку 'Отмена'").execute();
            autoSupportSession.sendInitialNode(userAccount.getUserId());
            autoSupportSession.onClose(() -> {
                clearUserMode(userAccount.getUserId());
                clearAutoSupportModel(userAccount.getUserId());
                try {
                    factory.userMainMenu("Главное меню").execute();
                } catch (TelegramApiException e) {
                    throw new SendingNodeException(e.getMessage());
                }
            });
        } catch (SendingNodeException | TelegramApiException e) {
            System.out.println(e.getMessage());
        }
        return true;
    }

    private boolean handleEnableDeferredPayment(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        TelegramMessageFactory factory = TelegramMessageFactory.create(message.getChatId(), mainBot);
        Message tempMsg = factory.simpleMessage("Подключение отложенного платежа...").execute();
        factory.userMainMenu("Главное меню").execute();
        try {
            apiBillingController.deferredPayment(userAccount.getUserLogin());
            factory.editTextOfMessage(tempMsg.getMessageId(), "Отложенный платеж подключен.").execute();
        } catch (Exception e) {
            factory.editTextOfMessage(tempMsg.getMessageId(), "Не удалось подключить отложенный платеж.").execute();
            return false;
        }
        return true;
    }

    private boolean handleResumeService(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        TelegramMessageFactory factory = TelegramMessageFactory.create(message.getChatId(), mainBot);
        Message tempMsg = factory.simpleMessage("Возобновление обслуживания...").execute();
        factory.userMainMenu("Главное меню").execute();
        try {
            apiBillingController.startUserService(userAccount.getUserLogin());
            factory.editTextOfMessage(tempMsg.getMessageId(), "Возобновление обслуживания прошло успешно.").execute();
        } catch (Exception e) {
            factory.editTextOfMessage(tempMsg.getMessageId(), "Не удалось возобновить обслуживание.").execute();
            return false;
        }
        return true;
    }

    private boolean handleListRemoveRequests(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        // Ищем нереализованные запросы
        List<UserRequest> existedRequests = userRequestDispatcher.getUnprocessedRequestsByLogin(userAccount.getUserLogin());
        TelegramMessageFactory.create(message.getChatId(), mainBot).listRemoveUserRequests(existedRequests).execute();
        return true;
    }

    private boolean handleConnectService(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        List<String> alreadyConnectedServices = userInfo.getOldTarif().stream().skip(1).map(ApiBillingController.OldTarifItem::getService).toList();
        List<UserTariff> services = userTariffRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isService")));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (!alreadyConnectedServices.isEmpty())
                predicates.add(root.get("baseName").in(alreadyConnectedServices).not());

            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.ASC, "isService", "price"));
        TelegramMessageFactory.create(message.getChatId(), mainBot).connectUserService(userInfo, services).execute();
        return true;
    }

    private boolean handleDisconnectService(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        TelegramMessageFactory.create(message.getChatId(), mainBot).disconnectUserService(userInfo).execute();
        return true;
    }

    private boolean handleTariffRequest(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        TelegramMessageFactory.create(message.getChatId(), mainBot).userTariffRequest(userInfo).execute();
        return true;
    }

    private boolean handleServicesRequest(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        TelegramMessageFactory.create(message.getChatId(), mainBot).userServicesRequest(userInfo).execute();
        return true;
    }

    private boolean handleBalanceRequest(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        TelegramMessageFactory.create(message.getChatId(), mainBot).userBalanceMenu(userInfo).execute();
        return true;
    }

    private boolean handleSuspendRequest(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        TelegramMessageFactory.create(message.getChatId(), mainBot).userSuspendConfirmMenu(userAccount).execute();
        return true;
    }

    private boolean handleShowMainMenu(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        clearUserMode(userAccount.getUserId());
        clearAutoSupportModel(userAccount.getUserId());
        TelegramMessageFactory.create(message.getChatId(), mainBot).userMainMenu("Главное меню").execute();
        return true;
    }

    private boolean handleChangeTariffRequest(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        String currentTariff = userInfo.getOldTarif().get(0).getService();

        List<UserTariff> services = userTariffRepository.findAll((root, query, cb) -> cb.and(cb.isFalse(root.get("deleted")), cb.isFalse(root.get("isService")), cb.notEqual(root.get("baseName"), currentTariff)), Sort.by(Sort.Direction.ASC, "isService", "price"));
        TelegramMessageFactory.create(message.getChatId(), mainBot).changeUserTariff(userInfo, services).execute();
        return true;
    }

    private boolean handleCreateReview(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        TelegramMessageFactory factory = TelegramMessageFactory.create(message.getChatId(), mainBot);
        List<Review> todayReviews = reviewDispatcher.getTodayReviews(userAccount.getUserLogin());
        if (todayReviews.size() > 4) {
            factory.simpleMessage("Превышено количество отправляемых отзывов, повторите попытку позже").execute();
            return true;
        }

        setUserMode(userAccount.getUserId(), Mode.REVIEW);

        factory.createReview().execute();

        return true;
    }

    private boolean handleSendReview(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        clearUserMode(userAccount.getUserId());
        TelegramMessageFactory factory = TelegramMessageFactory.create(message.getChatId(), mainBot);
        Review review = reviewDispatcher.createReview(userAccount.getUserLogin(), message.getText(), "Telegram");
        sendReviewNotification(review);
        stompController.updateUserReview(UpdateCarrier.from(EventType.CREATE, review));
        factory.userMainMenu("Отзыв отправлен").execute();
        return true;
    }

    public void setUserMode(Long userId, Mode mode) {
        usersInMode.compute(userId, (k, v) -> mode);
    }

    @Nullable
    public Mode getUserMode(Long userId) {
        return usersInMode.getOrDefault(userId, null);
    }

    public void clearUserMode(Long userId) {
        usersInMode.remove(userId);
    }

    public void setAutoSupportSession(Long userId, AutoSupportSession session) {
        autoSupportSessions.put(userId, session);
    }

    @Nullable
    public AutoSupportSession getAutoSupportModel(Long userId) {
        return autoSupportSessions.getOrDefault(userId, null);
    }

    public void clearAutoSupportModel(Long userId) {
        autoSupportSessions.remove(userId);
    }

    public Message sendMessage(SendMessage message) throws TelegramApiException {
        if (mainBot == null || message == null) return null;
        return mainBot.execute(message);
    }

    public List<Message> sendMediaGroup(NetworkMediaGroup message) throws TelegramApiException {
        if (mainBot == null || message == null) return null;
        AtomicBoolean first = new AtomicBoolean(true);

        SendMediaGroup mediaGroup = new SendMediaGroup(message.getUserId(), message.getFiles().stream().map((file) -> {
            InputMediaPhoto photo = new InputMediaPhoto();
            photo.setMedia(file.toInputStream(), file.getName());
            if (first.getAndSet(false)) {
                photo.setCaption(message.getCaption());
            }
            return photo;
        }).collect(Collectors.toList()));
        return mainBot.execute(mediaGroup);
    }

    public Message sendPhoto(NetworkSendPhoto message) throws TelegramApiException {
        if (mainBot == null || message == null) return null;
        final SendPhoto deserialized = new SendPhoto(message.getUserId(), new InputFile(message.getPhoto().toInputStream(), message.getPhoto().getName()));
        deserialized.setCaption(message.getCaption());
        return mainBot.execute(deserialized);
    }

    public Serializable editMessageText(EditMessageText message) throws TelegramApiException {
        if (mainBot == null || message == null) return null;
        return mainBot.execute(message);
    }

    public Serializable editMessageCaption(EditMessageCaption message) throws TelegramApiException {
        if (mainBot == null || message == null) return null;
        return mainBot.execute(message);
    }

    public Serializable deleteMessage(DeleteMessage message) throws TelegramApiException {
        if (mainBot == null || message == null) return null;
        return mainBot.execute(message);
    }

    public String getFile(GetFile getFile) throws TelegramApiException {
        if (mainBot == null || getFile == null) return null;
        return mainBot.execute(getFile).getFileUrl(telegramConf.getBotToken());
    }

    public void send(Long chatId, String message) {
        try {
            TelegramMessageFactory.create(chatId, mainBot).simpleMessage(message).execute();
        } catch (TelegramApiException e) {
            System.out.println("Не удалось отправить сообщение пользователю " + chatId);
        }
    }

    public enum Mode {
        REVIEW, AUTO_SUPPORT
    }

    @Getter
    @Setter
    public static class UserTelegramCredentials extends Credentials {
        private Long id;
        private String sc;

        public static UserTelegramCredentials from(Long userId, String login) {
            UserTelegramCredentials credentials = new UserTelegramCredentials();
            credentials.setId(userId);
            credentials.setLogin(login);
            return credentials;
        }
    }
}
