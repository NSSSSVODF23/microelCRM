package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.pon.schema.PonNode;
import com.microel.tdo.pon.schema.PonScheme;
import com.microel.tdo.pon.schema.forms.PonSchemeForm;
import com.microel.trackerbackend.services.external.pon.PonextenderClient;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.team.Employee;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("api/private/pon/scheme")
public class PonSchemesController {

    private final PonextenderClient ponClient;
    private final EmployeeDispatcher employeeDispatcher;

    public PonSchemesController(PonextenderClient ponClient, EmployeeDispatcher employeeDispatcher) {
        this.ponClient = ponClient;
        this.employeeDispatcher = employeeDispatcher;
    }


    @PostMapping("create")
    public ResponseEntity<PonScheme> createScheme(@RequestBody PonSchemeForm form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        form.setLogin(employee.getLogin());
        return ResponseEntity.ok(ponClient.createScheme(form));
    }

    @PatchMapping("{id}/update")
    public ResponseEntity<PonScheme> updateScheme(@PathVariable Long id, @RequestBody PonSchemeForm form, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        form.setLogin(employee.getLogin());
        return ResponseEntity.ok(ponClient.updateScheme(id, form));
    }

    @DeleteMapping("{id}/delete")
    public ResponseEntity<Void> deleteScheme(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        ponClient.deleteScheme(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("{id}")
    public ResponseEntity<PonScheme> getScheme(@PathVariable Long id) {
        return ResponseEntity.ok(ponClient.getSchemeById(id));
    }

    @GetMapping("list")
    public ResponseEntity<List<PonScheme>> getSchemes() {
        return ResponseEntity.ok(ponClient.getSchemes());
    }

    @PatchMapping("{id}/edit")
    public ResponseEntity<Void> editPonScheme(@PathVariable Long id, @RequestBody List<? extends PonNode> data, HttpServletRequest request) {
        Employee employee = employeeDispatcher.getEmployeeFromRequest(request);
        ponClient.editScheme(id, data, employee.getLogin());

        return ResponseEntity.ok().build();
    }

    @GetMapping("{id}/elements")
    public ResponseEntity<List<? extends PonNode>> getSchemeElements(@PathVariable Long id) {
        return ResponseEntity.ok(ponClient.getSchemeElements(id));
    }
}
