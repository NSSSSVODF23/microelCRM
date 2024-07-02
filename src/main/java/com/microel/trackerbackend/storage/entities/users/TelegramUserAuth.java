package com.microel.trackerbackend.storage.entities.users;

import com.microel.trackerbackend.controllers.telegram.UserTelegramController;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelegramUserAuth that)) return false;
        return Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getUserLogin(), that.getUserLogin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getUserLogin());
    }
}
