package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.billing.ApiBillingController;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.dispatchers.WorkLogDispatcher;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.ModelItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("api/private/work-log")
public class WorkLogRequestController {

    private final ApiBillingController apiBillingController;
    private final EmployeeDispatcher employeeDispatcher;
    private final ModelItemRepository modelItemRepository;
    private final StompController stompController;
    private final WorkLogDispatcher workLogDispatcher;

    public WorkLogRequestController(ApiBillingController apiBillingController, EmployeeDispatcher employeeDispatcher,
                                    ModelItemRepository modelItemRepository, StompController stompController, WorkLogDispatcher workLogDispatcher) {
        this.apiBillingController = apiBillingController;
        this.employeeDispatcher = employeeDispatcher;
        this.modelItemRepository = modelItemRepository;
        this.stompController = stompController;
        this.workLogDispatcher = workLogDispatcher;
    }

    // Получить WorkLog по его идентификатору
    @GetMapping("{workLogId}")
    public ResponseEntity<WorkLog> getWorkLog(@PathVariable Long workLogId) {
        try {
            return ResponseEntity.ok(workLogDispatcher.get(workLogId));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    // Получить журналы работ определенной задачи
    @GetMapping("task/{taskId}/list")
    public ResponseEntity<List<WorkLog>> getWorkLogs(@PathVariable Long taskId) {
        return ResponseEntity.ok(workLogDispatcher.getAllByTaskId(taskId));
    }

    /**
     *Получить список активных журналов работ
     */
    @GetMapping("active/list")
    public ResponseEntity<List<WorkLog>> getActiveWorkLogs() {
        return ResponseEntity.ok(workLogDispatcher.getActive());
    }

    // Получить количество активных журналов работ
    @GetMapping("active/count")
    public ResponseEntity<Long> getActiveWorkLogsCount() {
        return ResponseEntity.ok(workLogDispatcher.getActiveCount());
    }

    @GetMapping("task/{taskId}/active")
    public ResponseEntity<WorkLog> getActiveWorkLogsByTask(@PathVariable Long taskId) {
        try {
            return ResponseEntity.ok(workLogDispatcher.getActiveByTaskId(taskId));
        } catch (EntryNotFound e) {
            return ResponseEntity.ok(null);
        }
    }

    @GetMapping("uncalculated/list")
    public ResponseEntity<List<WorkLog>> getUncalculatedWorkLogs() {
        return ResponseEntity.ok(workLogDispatcher.getUncalculated());
    }

    @GetMapping("after-work/list")
    public ResponseEntity<List<WorkLog>> getAfterWorkList(HttpServletRequest request) {
        return ResponseEntity.ok(workLogDispatcher.getAfterWork(employeeDispatcher.getEmployeeFromRequest(request)));
    }

}
