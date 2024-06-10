package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.EventType;
import com.microel.tdo.UpdateCarrier;
import com.microel.trackerbackend.controllers.telegram.UserTelegramController;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.EmployeeIntervention;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.userstlg.TelegramUserAuth;
import com.microel.trackerbackend.storage.entities.userstlg.UserRequest;
import com.microel.trackerbackend.storage.entities.userstlg.UserTariff;
import com.microel.trackerbackend.storage.repositories.TelegramUserAuthRepository;
import com.microel.trackerbackend.storage.repositories.UserRequestRepository;
import com.microel.trackerbackend.storage.repositories.UserTariffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("api/private/user/telegram")
public class UserTelegramRequestController {
    private final UserTariffRepository userTariffRepository;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;
    private final UserRequestRepository userRequestRepository;
    private final UserTelegramController userTelegramController;
    private final TelegramUserAuthRepository telegramUserAuthRepository;

    public UserTelegramRequestController(UserTariffRepository userTariffRepository, EmployeeDispatcher employeeDispatcher, StompController stompController, UserRequestRepository userRequestRepository, UserTelegramController userTelegramController, TelegramUserAuthRepository telegramUserAuthRepository) {
        this.userTariffRepository = userTariffRepository;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
        this.userRequestRepository = userRequestRepository;
        this.userTelegramController = userTelegramController;
        this.telegramUserAuthRepository = telegramUserAuthRepository;
    }

    @GetMapping("tariffs")
    public ResponseEntity<List<UserTariff>> getTlgUserTariffs() {
        return ResponseEntity.ok(userTariffRepository.findAll((root, query, cb) -> cb.isFalse(root.get("deleted")),
                Sort.by(Sort.Direction.ASC, "isService", "price")));
    }

    @PostMapping("tariff")
    public ResponseEntity<UserTariff> addTlgUserTariff(@RequestBody UserTariff.Form form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        UserTariff userTariff = userTariffRepository.save(UserTariff.of(form, employee));
        UpdateCarrier<UserTariff> updateCarrier = UpdateCarrier.from(EventType.CREATE, userTariff);
        stompController.updateTlgUserTariff(updateCarrier);
        return ResponseEntity.ok(userTariff);
    }

    @PatchMapping("tariff/{id}")
    public ResponseEntity<UserTariff> updateTlgUserTariff(@RequestBody UserTariff.Form form, @PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        UserTariff userTariff = userTariffRepository.findById(id).orElseThrow(() -> new ResponseException("Тариф не найден"));
        userTariff.update(form, employee);
        UpdateCarrier<UserTariff> updateCarrier = UpdateCarrier.from(EventType.UPDATE, userTariff);
        stompController.updateTlgUserTariff(updateCarrier);
        return ResponseEntity.ok(userTariffRepository.save(userTariff));
    }

    @DeleteMapping("tariff/{id}")
    public ResponseEntity<UserTariff> deleteTlgUserTariff(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        UserTariff userTariff = userTariffRepository.findById(id).orElseThrow(() -> new ResponseException("Тариф не найден"));
        userTariff.delete(employee);
        UpdateCarrier<UserTariff> updateCarrier = UpdateCarrier.from(EventType.DELETE, userTariff);
        stompController.updateTlgUserTariff(updateCarrier);
        return ResponseEntity.ok(userTariffRepository.save(userTariff));
    }

    @GetMapping("requests")
    public ResponseEntity<Page<UserRequest>> getTlgUserRequests(@RequestParam Integer page, @RequestParam Integer size, @RequestParam Boolean unprocessed, @RequestParam(required = false) String login) {
        Page<UserRequest> pageRequests = userRequestRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates  = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            if(unprocessed)
                predicates.add(cb.isNull(root.get("processedBy")));
            else
                predicates.add(cb.isNotNull(root.get("processedBy")));

            if(login != null  &&!login.isBlank())
                predicates.add(cb.like(cb.lower(root.get("userLogin")), "%" + login.toLowerCase() + "%"));

            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created")));
        return ResponseEntity.ok(pageRequests);
    }

    @GetMapping("request/unprocessed/count")
    public ResponseEntity<Long> getTlgUserRequestsCount() {
        return ResponseEntity.ok(userRequestRepository.count((root, query, cb) -> cb.isNull(root.get("processedBy"))));
    }

    @PatchMapping("request/{id}/processed")
    public ResponseEntity<Void> updateTlgUserRequestProcessed(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String userMessage = body.get("userMessage");
        if (userMessage == null || userMessage.isBlank()) throw new ResponseException("Не указан текст сообщения");
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        UserRequest userRequest = userRequestRepository.findById(id).orElseThrow(() -> new ResponseException("Запрос пользователя не найден"));
        String userLogin = userRequest.getUserLogin();
        userRequest.setProcessedBy(EmployeeIntervention.from(employee, userMessage));
        userRequest = userRequestRepository.save(userRequest);
        stompController.updateTlgUserRequest(UpdateCarrier.from(EventType.UPDATE, userRequest));
        List<TelegramUserAuth> userAuthList = telegramUserAuthRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("userLogin"), userLogin)
        ));
        for (TelegramUserAuth userAuth : userAuthList) {
            userTelegramController.send(userAuth.getUserId(), userMessage);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("new-chat/{login}")
    public ResponseEntity<List<UUID>> createNewChat(@PathVariable String login) {
        List<TelegramUserAuth> userAuthList = telegramUserAuthRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("userLogin"), login)
        ));
        if (userAuthList.isEmpty()) throw new ResponseException("Пользователь не авторизован в Telegram");
        return ResponseEntity.ok(
                userAuthList.stream()
                        .map(auth -> userTelegramController.createNewChat(auth.getUserId(), auth.getUserLogin()))
                        .toList()
        );
    }
}
