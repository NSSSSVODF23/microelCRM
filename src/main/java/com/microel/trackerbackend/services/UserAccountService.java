package com.microel.trackerbackend.services;

import com.microel.trackerbackend.controllers.telegram.UserTelegramController;
import com.microel.trackerbackend.storage.entities.users.TelegramUserAuth;
import com.microel.trackerbackend.storage.repositories.TelegramUserAuthRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UserAccountService {
    private final TelegramUserAuthRepository telegramUserAuthRepository;

    public UserAccountService(TelegramUserAuthRepository telegramUserAuthRepository) {
        this.telegramUserAuthRepository = telegramUserAuthRepository;
    }

    public boolean isCredentialsValid(String username, String password){
        Connection connect = Jsoup.connect("http://user.vdonsk.ru/");
        connect.data(
                Map.of(
                        "login", username,
                        "passwd", password,
                        "logon", "1"
                )
        );
        try {
            Document post = connect.post();
            return post.text().contains("Здравствуйте");
        } catch (IOException ignored){
            return false;
        }
    }

    @Transactional
    public void doSaveUserAccount(UserTelegramController.UserTelegramCredentials credentials) {
        List<TelegramUserAuth> userId = telegramUserAuthRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("userId"), credentials.getId())
        ));
        if(userId.isEmpty()){
            telegramUserAuthRepository.save(TelegramUserAuth.of(credentials));
        }else{
            userId.get(0).setUserLogin(credentials.getLogin());
            telegramUserAuthRepository.save(userId.get(0));
        }
    }

    @Nullable
    public TelegramUserAuth getUserAccount(Long userId){
        return telegramUserAuthRepository.findAll((root, query, cb) -> cb.equal(root.get("userId"), userId)).stream().findFirst().orElse(null);
    }
}
