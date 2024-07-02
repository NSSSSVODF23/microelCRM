package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.users.UserTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserTariffRepository extends JpaRepository<UserTariff, Long>, JpaSpecificationExecutor<UserTariff> {
}
