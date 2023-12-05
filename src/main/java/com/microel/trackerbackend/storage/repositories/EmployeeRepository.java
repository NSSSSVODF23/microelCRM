package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.team.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EmployeeRepository extends JpaRepository<Employee, String>, JpaSpecificationExecutor<Employee> {
    boolean existsByLoginAndDeletedEquals(String login, boolean deleted);

    List<Employee> findAllByOffsiteIsTrue();

    List<Employee> findAllByLoginIsInAndDeletedIsFalseAndOffsiteIsFalse(Set<String> personalResponsibilities);

    Set<Employee> findAllByLoginInAndDeletedIsFalseAndOffsiteIsFalse(List<String> collect);

    Optional<Employee> findByTelegramUserId(String chatId);

    Optional<Employee> findTopByTelegramUserId(String chatId);

    List<Employee> findAllByOffsiteIsTrueAndDeletedIsFalse();
}
