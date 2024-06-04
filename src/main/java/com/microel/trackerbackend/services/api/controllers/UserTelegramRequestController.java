package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.EventType;
import com.microel.tdo.UpdateCarrier;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.userstlg.UserRequest;
import com.microel.trackerbackend.storage.entities.userstlg.UserTariff;
import com.microel.trackerbackend.storage.repositories.UserRequestRepository;
import com.microel.trackerbackend.storage.repositories.UserTariffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("api/private/user/telegram")
public class UserTelegramRequestController {
    private final UserTariffRepository userTariffRepository;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;
    private final UserRequestRepository userRequestRepository;

    public UserTelegramRequestController(UserTariffRepository userTariffRepository, EmployeeDispatcher employeeDispatcher, StompController stompController, UserRequestRepository userRequestRepository) {
        this.userTariffRepository = userTariffRepository;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
        this.userRequestRepository = userRequestRepository;
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
        UpdateCarrier<UserTariff> updateCarrier  = UpdateCarrier.from(EventType.DELETE, userTariff);
        stompController.updateTlgUserTariff(updateCarrier);
        return ResponseEntity.ok(userTariffRepository.save(userTariff));
    }

    @GetMapping("requests")
    public ResponseEntity<Page<UserRequest>> getTlgUserRequests(@RequestParam Integer page, @RequestParam Integer size, @RequestParam Boolean unprocessed)  {
        Page<UserRequest> pageRequests  = userRequestRepository.findAll((root, query, cb) -> cb.and(
                unprocessed ? cb.isNull(root.get("processedBy")) : cb.isNotNull(root.get("processedBy"))
        ), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,  "created")));
        return ResponseEntity.ok(pageRequests);
    }
}
