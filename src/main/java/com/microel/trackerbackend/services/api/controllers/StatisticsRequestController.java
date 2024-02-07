package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.storage.dispatchers.StatisticsDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("api/private/statistics")
public class StatisticsRequestController {

    private final StatisticsDispatcher statisticsDispatcher;

    public StatisticsRequestController(StatisticsDispatcher statisticsDispatcher) {
        this.statisticsDispatcher = statisticsDispatcher;
    }

    @PostMapping("employee-work")
    public ResponseEntity<StatisticsDispatcher.EmployeeWorkStatisticsTable> getEmployeeWorkStatistics(@RequestBody StatisticsDispatcher.EmployeeWorkStatisticsForm form) {
        return ResponseEntity.ok(statisticsDispatcher.getEmployeeWorkStatistics(form));
    }
}
