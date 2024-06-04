package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.userstlg.TelegramUserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TelegramUserAuthRepository extends JpaRepository<TelegramUserAuth, Long>, JpaSpecificationExecutor<TelegramUserAuth> {
}
