package com.microel.trackerbackend.controllers.telegram;

import com.microel.tdo.EventType;
import com.microel.tdo.UpdateCarrier;
import com.microel.trackerbackend.controllers.configuration.Configuration;
import com.microel.trackerbackend.controllers.configuration.FailedToReadConfigurationException;
import com.microel.trackerbackend.controllers.configuration.FailedToWriteConfigurationException;
import com.microel.trackerbackend.controllers.configuration.entity.UserTelegramConf;
import com.microel.trackerbackend.controllers.telegram.reactor.TelegramCallbackReactor;
import com.microel.trackerbackend.controllers.telegram.reactor.TelegramChatJoinReactor;
import com.microel.trackerbackend.controllers.telegram.reactor.TelegramCommandReactor;
import com.microel.trackerbackend.controllers.telegram.reactor.TelegramMessageReactor;
import com.microel.trackerbackend.modules.transport.Credentials;
import com.microel.trackerbackend.services.UserAccountService;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.entities.userstlg.TelegramUserAuth;
import com.microel.trackerbackend.storage.entities.userstlg.UserRequest;
import com.microel.trackerbackend.storage.entities.userstlg.UserTariff;
import com.microel.trackerbackend.storage.repositories.UserRequestRepository;
import com.microel.trackerbackend.storage.repositories.UserTariffRepository;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserTelegramController {
    private final Configuration configuration;
    private final StompController stompController;
    private final UserAccountService userAccountService;
    private final String authSalt = "3xv23sj*NYDgNHk43m(Z1P6P4:PW?s/i";
    private final ApiBillingController apiBillingController;
    private final UserTariffRepository userTariffRepository;
    private UserTelegramConf telegramConf;
    @Nullable
    private TelegramBotsApi api;
    @Nullable
    private MainBot mainBot;
    @Nullable
    private BotSession mainBotSession;
    private final UserRequestRepository userRequestRepository;

    public UserTelegramController(Configuration configuration, StompController stompController, UserAccountService userAccountService, ApiBillingController apiBillingController, UserTariffRepository userTariffRepository,
                                  UserRequestRepository userRequestRepository) {
        this.configuration = configuration;
        this.stompController = stompController;
        this.userAccountService = userAccountService;
        try {
            telegramConf = configuration.load(UserTelegramConf.class);
            initializeMainBot();
        } catch (FailedToReadConfigurationException e) {
            System.out.println("Конфигурация для Telegram не найдена");
        } catch (TelegramApiException e) {
            System.out.println("Ошибка Telegram API " + e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        this.apiBillingController = apiBillingController;
        this.userTariffRepository = userTariffRepository;
        this.userRequestRepository = userRequestRepository;
    }

    private String hashWithSalt(Long input) {
        String saltedInput = input + authSalt;
        byte[] hash = DigestUtils.sha256(saltedInput);
        return Hex.encodeHexString(hash);
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

        mainBot.subscribe(new TelegramCallbackReactor("connect_user_service", (update, callbackData) -> {
            if (callbackData  == null) return false;
            final Long chatId  = update.getCallbackQuery().getFrom().getId();
            final Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            final TelegramUserAuth userAccount = userAccountService.getUserAccount(chatId);
            TelegramMessageFactory factory  = TelegramMessageFactory.create(chatId, mainBot);
            if (userAccount == null) {
                factory.linkForUserAuth(chatId, hashWithSalt(chatId)).execute();
                return false;
            }
            String requestDescription = String.format("Пользователь %s хочет подключить услугу %s", userAccount.getUserLogin(), callbackData.getString());
            // Ищем похожие нереализованные запросы
            List<UserRequest> existedRequests= userRequestRepository.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("description"), requestDescription),
                    cb.isNull(root.get("processedBy")),
                    cb.isFalse(root.get("deleted"))
            ));
            if(!existedRequests.isEmpty()) {
                factory.userRequestResponse(messageId, "Вы уже оставили запрос на подключение доп. услуги").execute();
                return false;
            }
            UserRequest userRequest = UserRequest.of(userAccount.getUserLogin(), UserRequest.Type.APPEND_SERVICE, requestDescription, "telegram");
            userRequest = userRequestRepository.save(userRequest);
            factory.userRequestResponse(messageId, "Успешно создан запрос на подключение доп. услуги").execute();
            stompController.updateTlgUserRequest(UpdateCarrier.from(EventType.CREATE, userRequest));
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
            if (handleMainMenuCommand(message, userAccount)) return true;
            return true;
        }));
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
            case UserMenuCommands.MAIN_MENU -> {
                return handleShowMainMenu(message, userAccount);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleConnectService(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        List<String> alreadyConnectedServices  = userInfo.getOldTarif().stream().skip(1)
                .map(ApiBillingController.OldTarifItem::getService)
                .toList();
        List<UserTariff> services = userTariffRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.isFalse(root.get("deleted")),
                        cb.isTrue(root.get("isService")),
                        root.get("baseName").in(alreadyConnectedServices).not()
                ),
                Sort.by(Sort.Direction.ASC, "isService", "price")
        );
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
        TelegramMessageFactory.create(message.getChatId(), mainBot).userTariffRequest(userInfo).execute();
        return true;
    }

    private boolean handleSuspendRequest(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        ApiBillingController.TotalUserInfo userInfo = apiBillingController.getUserInfo(userAccount.getUserLogin());
        TelegramMessageFactory.create(message.getChatId(), mainBot).userTariffRequest(userInfo).execute();
        return true;
    }

    private boolean handleShowMainMenu(Message message, TelegramUserAuth userAccount) throws TelegramApiException {
        TelegramMessageFactory.create(message.getChatId(), mainBot).userMainMenu("Главное меню").execute();
        return true;
    }

    @Getter
    @Setter
    public static class UserTelegramCredentials extends Credentials {
        private Long id;
        private String sc;
    }
}
