package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateSubscribe;
import com.microel.trackerbackend.controllers.telegram.reactor.*;
import com.microel.trackerbackend.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MainBot extends TelegramLongPollingBot {
    public Map<UUID, TelegramUpdateSubscribe> reactors = new ConcurrentHashMap<>();
    private TelegramConf configuration;

    public MainBot(TelegramConf conf) {
        super(conf.getBotToken());
        configuration = conf;
    }

    public TelegramUpdateSubscribe subscribe(TelegramUpdateReactor reactor, Boolean once) {
        UUID id = UUID.randomUUID();
        TelegramUpdateSubscribe subscribe = new TelegramUpdateSubscribe();
        subscribe.setReactor(reactor);
        subscribe.setIsOnce(once);
        subscribe.setBotInstance(this);
        subscribe.setSubscriptionId(id);
        reactors.put(id, subscribe);
        return subscribe;
    }

    public TelegramUpdateSubscribe subscribe(TelegramUpdateReactor reactor) {
        return subscribe(reactor, false);
    }

    @Nullable
    public <T extends java.io.Serializable, Method extends org.telegram.telegrambots.meta.api.methods.BotApiMethod<T>> T send(Method method) {
        try {
            return execute(method);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить сообщение в Telegram пользователю:" + e.getMessage());
        }
        return null;
    }

    public void unsubscribe(UUID uuid) {
        reactors.remove(uuid);
    }

    @Override
    public void onUpdateReceived(Update update) {
        List<TelegramReactorType> sortExample = List.of(TelegramReactorType.COMMAND, TelegramReactorType.PROMPT, TelegramReactorType.CALLBACK, TelegramReactorType.MESSAGE, TelegramReactorType.EDIT_MESSAGE);
        List<TelegramUpdateSubscribe> subscriptions = reactors.values().stream().sorted(Comparator.comparing(o -> sortExample.indexOf(o.getReactor().getType()))).toList();
        for (TelegramUpdateSubscribe subscribe : subscriptions) {
            boolean isHandled = false;
            try {
                TelegramUpdateReactor reactor = subscribe.getReactor();
                switch (reactor.getType()) {
                    case COMMAND -> {
                        TelegramCommandReactor cmdReactor = (TelegramCommandReactor) reactor;
                        if (update.hasMessage() && update.getMessage().isCommand()) {
                            if (!cmdReactor.getTargetCommand().equals(update.getMessage().getText())) continue;
                            if ((isHandled = cmdReactor.getHandler().handle(update)) && subscribe.getIsOnce())
                                subscribe.unsubscribe();
                        }
                    }
                    case PROMPT -> {
                        TelegramPromptReactor promptReactor = (TelegramPromptReactor) reactor;
                        if (update.hasMessage() && !update.getMessage().isCommand() && promptReactor.getTargetPrompt().equals(update.getMessage().getText())) {
                            if ((isHandled = promptReactor.getHandler().handle(update)) && subscribe.getIsOnce())
                                subscribe.unsubscribe();
                        }
                    }
                    case CALLBACK -> {
                        TelegramCallbackReactor callbackReactor = (TelegramCallbackReactor) reactor;
                        if (update.hasCallbackQuery()) {
                            if (callbackReactor.getPrefix() != null) {
                                try {
                                    CallbackData callbackData = CallbackData.parse(update.getCallbackQuery().getData());
                                    if (callbackData.isPrefix(callbackReactor.getPrefix())) {
                                        if ((isHandled = callbackReactor.getHandler().handle(update, callbackData)) && subscribe.getIsOnce())
                                            subscribe.unsubscribe();
                                        break;
                                    }
                                } catch (IllegalArgumentException ignore) {
                                }
                            } else {
                                if (callbackReactor.getHandler().handle(update, null) && subscribe.getIsOnce())
                                    subscribe.unsubscribe();
                            }
                        }
                    }
                    case MESSAGE -> {
                        TelegramMessageReactor messageReactor = (TelegramMessageReactor) reactor;
                        if (update.hasMessage() && !update.getMessage().isCommand() && update.getMessage().getChat().isUserChat()) {
                            if ((isHandled = messageReactor.getHandler().handle(update)) && subscribe.getIsOnce())
                                subscribe.unsubscribe();
                        }
                    }
                    case EDIT_MESSAGE -> {
                        TelegramEditMessageReactor editMessageReactor = (TelegramEditMessageReactor) reactor;
                        if (update.hasEditedMessage() && update.getMessage().getChat().isUserChat()) {
                            if ((isHandled = editMessageReactor.getHandler().handle(update)) && subscribe.getIsOnce())
                                subscribe.unsubscribe();
                        }
                    }
                }
                if (isHandled)
                    break;
            } catch (CustomException | TelegramApiException e) {
                Long chatId = null;
                if (update.hasMessage()) {
                    chatId = update.getMessage().getChatId();
                }else if(update.hasCallbackQuery()){
                    chatId = update.getCallbackQuery().getMessage().getChatId();
                }
                if(chatId != null) {
                    try {
                        TelegramMessageFactory.create(chatId, this).simpleMessage(e.getMessage()).execute();
                    } catch (TelegramApiException ignored) {
                    }
                }
            }
        }

    }

    @Override
    public String getBotUsername() {
        return configuration.getBotName();
    }

    public String getToken(){
        return configuration.getBotToken();
    }

}
