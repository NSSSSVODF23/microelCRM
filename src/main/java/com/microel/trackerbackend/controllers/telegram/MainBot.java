package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.controllers.configuration.entity.TelegramConf;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainBot extends TelegramLongPollingBot {
    private String botUsername = "";
    public Map<UUID, TelegramUpdateSubscribe> reactors = new HashMap<>();
    public MainBot(TelegramConf conf) {
        super(conf.getBotToken());
        botUsername = conf.getBotName();
    }

    public TelegramUpdateSubscribe subscribe(TelegramUpdateReactor reactor, Boolean once){
        UUID id = UUID.randomUUID();
        TelegramUpdateSubscribe subscribe = new TelegramUpdateSubscribe();
        subscribe.setReactor(reactor);
        subscribe.setIsOnce(once);
        subscribe.setBotInstance(this);
        subscribe.setSubscriptionId(id);
        reactors.put(id, subscribe);
        return subscribe;
    }

    public TelegramUpdateSubscribe subscribe(TelegramUpdateReactor reactor){
        return subscribe(reactor, false);
    }

    public void unsubscribe(UUID uuid){
        reactors.remove(uuid);
    }

    @Override
    public void onUpdateReceived(Update update) {
        for(TelegramUpdateSubscribe subscribe : reactors.values()){
            TelegramUpdateReactor reactor = subscribe.getReactor();
            switch (reactor.getType()){
                case COMMAND:
                    TelegramCommandReactor cmdReactor = (TelegramCommandReactor) reactor;
                    if(update.hasMessage() && update.getMessage().isCommand()){
                        if(!cmdReactor.getTargetCommand().equals(update.getMessage().getText())) continue;
                        reactor.getHandler().handle(update);
                        if(subscribe.getIsOnce()) subscribe.unsubscribe();
                    }
                    break;
                case PROMPT:
                    if(update.hasMessage() && !update.getMessage().isCommand()){
                        reactor.getHandler().handle(update);
                        if(subscribe.getIsOnce()) subscribe.unsubscribe();
                    }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
