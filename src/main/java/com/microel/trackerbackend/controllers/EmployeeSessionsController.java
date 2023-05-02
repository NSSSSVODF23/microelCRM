package com.microel.trackerbackend.controllers;

import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class EmployeeSessionsController {
    private final AuthorizationProvider authorizationProvider;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;

    // Список подключенных сессий сотрудников
    private final Map<String, Set<String>> sessions = new HashMap<>();

    public EmployeeSessionsController(AuthorizationProvider authorizationProvider,
                                      EmployeeDispatcher employeeDispatcher, @Lazy StompController stompController) {
        this.authorizationProvider = authorizationProvider;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
    }

    /**
     * Проверяет на валидность токен пользователя, достает из него логин, добавляет сессию в коллекцию сессий.
     * После обновляет статус сотрудника в базе.
     * @param authToken Токен пользователя
     * @param sessionId Идентификатор сессии выданный Stomp
     * @return Возвращает true если сессия успешно добавлена и это первая сессия сотрудника.
     * Нужно для генерации сообщения об обновление пользователя в браузере.
     */
    public boolean addSession(String authToken, String sessionId) {
        // Валидируем токен
        if (!authorizationProvider.tokenValidate(authToken, false)) return false;
        // Получаем логин из токена
        String loginFromToken = authorizationProvider.getLoginFromToken(authToken);
        if (loginFromToken == null) return false;
        // Вставляем идентификатор сессии если уже присутствует логин
        sessions.computeIfPresent(loginFromToken, (k, v) -> {
            v.add(sessionId);
            return v;
        });
        // Иначе добавляем новую коллекцию сессий с этим токеном
        Set<String> previousSessions = sessions.putIfAbsent(loginFromToken, Stream.of(sessionId).collect(Collectors.toSet()));
        // Обновляем статус сотрудника в базе
        if (previousSessions == null) {
            try {
                stompController.updateEmployee(
                        employeeDispatcher.setOnline(loginFromToken)
                );
                return true;
            } catch (EntryNotFound ignored) {
            }
        }
        return false;
    }

    /**
     * Удаляет сессию сотрудника, если сессия последняя то удаляет ключ из коллекции.
     * @param sessionId Идентификатор сессии
     * @return Возвращает true если удаленная сессия была последней сессией сотрудника.
     */
    public boolean removeSession(String sessionId) {
        for (Map.Entry<String, Set<String>> entry : sessions.entrySet()) {
            if (entry.getValue().remove(sessionId)) {
                if (entry.getValue().isEmpty()) {
                    sessions.remove(entry.getKey());
                    try {
                        stompController.updateEmployee(
                                employeeDispatcher.setOffline(entry.getKey())
                        );
                        return true;
                    }catch (EntryNotFound ignored) {
                    }
                }
                break;
            }
        }
        return false;
    }
}
