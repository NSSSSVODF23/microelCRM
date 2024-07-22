package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.storage.dispatchers.ContractsDispatcher;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.task.TypesOfContracts;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("api/private/contract")
public class ContractsRequestController {

    private final ContractsDispatcher contractsDispatcher;
    private final EmployeeDispatcher employeeDispatcher;

    public ContractsRequestController(ContractsDispatcher contractsDispatcher, EmployeeDispatcher employeeDispatcher) {
        this.contractsDispatcher = contractsDispatcher;
        this.employeeDispatcher = employeeDispatcher;
    }

    @GetMapping("type/list")
    public ResponseEntity<List<TypesOfContracts>> getContractTypesList() {
        return ResponseEntity.ok(contractsDispatcher.getTypesOfContracts());
    }

    @GetMapping("type/suggestion/list")
    public ResponseEntity<List<TypesOfContracts.Suggestion>> getContractTypesSuggestionList(String query) {
        return ResponseEntity.ok(contractsDispatcher.getTypesOfContractsSuggestions(query));
    }

    @PostMapping("type")
    public ResponseEntity<TypesOfContracts> createContractType(@RequestBody TypesOfContracts.Form form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(contractsDispatcher.createContractType(form, employee));
    }

    @PatchMapping("type/{id}")
    public ResponseEntity<TypesOfContracts> updateContractType(@PathVariable Long id, @RequestBody TypesOfContracts.Form form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        return ResponseEntity.ok(contractsDispatcher.updateContractType(id, form, employee));
    }

    @DeleteMapping("type/{id}")
    public ResponseEntity<Void> removeContractType(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        contractsDispatcher.removeContractType(id, employee);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("mark/received")
    public ResponseEntity<Void> markContractsAsReceived(@RequestBody List<Long> contractIds, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        contractsDispatcher.receiveContractCheck(contractIds, employee);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("mark/archived")
    public ResponseEntity<Void> markContractsAsArchived(@RequestBody List<Long> contractIds, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        contractsDispatcher.archiveContractCheck(contractIds, employee);
        return ResponseEntity.ok().build();
    }
}
