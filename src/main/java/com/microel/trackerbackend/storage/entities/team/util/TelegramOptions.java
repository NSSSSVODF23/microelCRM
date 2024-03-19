package com.microel.trackerbackend.storage.entities.team.util;

import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.CascadeType;

@Getter
@Setter
@Entity
@Table(name = "telegram_options")
public class TelegramOptions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @OneToOne()
    private Employee employee;
    @Nullable
    private String trackTerminal;

    public static TelegramOptions createDefault() {
        TelegramOptions telegramOptions = new TelegramOptions();
        return telegramOptions;
    }
}
