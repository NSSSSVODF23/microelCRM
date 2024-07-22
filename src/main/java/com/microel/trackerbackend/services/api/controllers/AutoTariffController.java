package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.dynamictable.TablePaging;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.tariff.AutoTariff;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.repositories.AutoTariffRepository;
import com.microel.trackerbackend.storage.repositories.TaskStageRepository;
import com.microel.trackerbackend.storage.repositories.WireframeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("api/private/auto-tariff")
public class AutoTariffController {
    private final TaskStageRepository taskStageRepository;
    private final WireframeRepository wireframeRepository;
    private final AutoTariffRepository autoTariffRepository;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;

    public AutoTariffController(AutoTariffRepository autoTariffRepository, EmployeeDispatcher employeeDispatcher,
                                WireframeRepository wireframeRepository,
                                TaskStageRepository taskStageRepository, StompController stompController) {
        this.autoTariffRepository = autoTariffRepository;
        this.employeeDispatcher = employeeDispatcher;
        this.wireframeRepository = wireframeRepository;
        this.taskStageRepository = taskStageRepository;
        this.stompController = stompController;
    }

    @PostMapping("list")
    public ResponseEntity<Page<AutoTariff>> getAutoTariffs(@RequestBody TablePaging paging) {
        return ResponseEntity.ok(autoTariffRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, paging.toPageRequest("")));
    }

    @PostMapping("create")
    public ResponseEntity<AutoTariff> createAutoTariff(@RequestBody AutoTariff.Form form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        Wireframe targetClass = wireframeRepository.findById(form.getTargetClassId()).orElseThrow(() -> new ResponseException("Не найден класс задачи"));
        TaskStage targetType = taskStageRepository.findById(form.getTargetType()).orElseThrow(() -> new ResponseException("Не найден тип задачи"));
        AutoTariff save = autoTariffRepository.save(AutoTariff.of(form, targetClass, targetType, employee));
        stompController.sendUpdateAutoTariff();
        return ResponseEntity.ok(save);
    }

    @PatchMapping("{id}")
    public ResponseEntity<AutoTariff> updateAutoTariff(@PathVariable String id, @RequestBody AutoTariff.Form form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        Wireframe targetClass = wireframeRepository.findById(form.getTargetClassId()).orElseThrow(() -> new ResponseException("Не найден класс задачи"));
        TaskStage targetType = taskStageRepository.findById(form.getTargetType()).orElseThrow(() -> new ResponseException("Не найден тип задачи"));
        AutoTariff targetAutoTariff = autoTariffRepository.findAll((root, query, cb) -> cb.and(cb.equal(root.get("autoTariffId"), id), cb.isFalse(root.get("isDeleted"))))
                .stream().findFirst().orElseThrow(() -> new ResponseException("Не найден авто-тариф"));
        AutoTariff save = autoTariffRepository.save(targetAutoTariff.update(form, targetClass, targetType, employee));
        stompController.sendUpdateAutoTariff();
        return ResponseEntity.ok(save);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<AutoTariff> deleteAutoTariff(@PathVariable String id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        AutoTariff targetAutoTariff = autoTariffRepository.findAll((root, query, cb) -> cb.and(cb.equal(root.get("autoTariffId"), id), cb.isFalse(root.get("isDeleted"))))
                .stream().findFirst().orElseThrow(() -> new ResponseException("Не найден авто-тариф"));
        AutoTariff save = autoTariffRepository.save(targetAutoTariff.delete(employee));
        stompController.sendUpdateAutoTariff();
        return ResponseEntity.ok(save);
    }

}
