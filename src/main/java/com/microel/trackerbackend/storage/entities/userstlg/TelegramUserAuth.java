package com.microel.trackerbackend.storage.entities.userstlg;

import com.microel.trackerbackend.controllers.telegram.UserTelegramController;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "telegram_user_auth")
public class TelegramUserAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long telegramUserAuthId;
    private Long userId;
    private String userLogin;
    private Timestamp authTime;

    public static TelegramUserAuth of(UserTelegramController.UserTelegramCredentials credentials){
        final TelegramUserAuth telegramUserAuth = new TelegramUserAuth();
        telegramUserAuth.setUserId(credentials.getId());
        telegramUserAuth.setUserLogin(credentials.getLogin());
        telegramUserAuth.setAuthTime(Timestamp.from(Instant.now()));
        return telegramUserAuth;
    }
}
