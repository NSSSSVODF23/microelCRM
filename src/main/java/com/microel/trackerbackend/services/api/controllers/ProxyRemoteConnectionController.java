package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.services.RemoteTelnetService;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("api/private/remote")
public class ProxyRemoteConnectionController {

    private final RemoteTelnetService remoteTelnetService;
    private final EmployeeDispatcher employeeDispatcher;

    public ProxyRemoteConnectionController(RemoteTelnetService remoteTelnetService, EmployeeDispatcher employeeDispatcher) {
        this.remoteTelnetService = remoteTelnetService;
        this.employeeDispatcher = employeeDispatcher;
    }

    @PostMapping("telnet/connect")
    private ResponseEntity<Void> connectToSession(@RequestBody ConnectionCredentials credentials, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        remoteTelnetService.connectMessage(credentials, employee);
        return ResponseEntity.ok().build();
    }

    @PostMapping("telnet/{sessionId}")
    public ResponseEntity<Void> sendTelnetDataToSession(@PathVariable String sessionId, @RequestBody String data) {
        remoteTelnetService.sendData(data, sessionId);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ConnectionCredentials {
        private String name;
        private String ip;
        private String sessionId;
    }
}
