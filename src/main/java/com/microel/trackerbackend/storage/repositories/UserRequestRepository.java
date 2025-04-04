package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.users.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRequestRepository extends JpaRepository<UserRequest, Long>, JpaSpecificationExecutor<UserRequest> {
}
