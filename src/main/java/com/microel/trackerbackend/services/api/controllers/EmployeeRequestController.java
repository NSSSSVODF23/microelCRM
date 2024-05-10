package com.microel.trackerbackend.services.api.controllers;

import com.microel.trackerbackend.services.PhyPhoneService;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.dispatchers.EmployeeDispatcher;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.NotificationSettings;
import com.microel.trackerbackend.storage.entities.team.util.EmployeeForm;
import com.microel.trackerbackend.storage.entities.team.util.EmployeeStatus;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EditingNotPossible;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("api/private/employee")
public class EmployeeRequestController {

    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;
    private final PhyPhoneService phyPhoneService;

    public EmployeeRequestController(EmployeeDispatcher employeeDispatcher, StompController stompController, PhyPhoneService phyPhoneService) {
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
        this.phyPhoneService = phyPhoneService;
    }

    /**
     * Получает список сотрудников
     *
     * @param globalFilter
     * @param showDeleted
     * @param showOffsite
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Employee>> getEmployeesList(@RequestParam @Nullable String globalFilter, @RequestParam @Nullable Boolean showDeleted, @RequestParam @Nullable Boolean showOffsite) {
        if (globalFilter != null && !globalFilter.isBlank() || showDeleted != null || showOffsite != null)
            return ResponseEntity.ok(employeeDispatcher.getEmployeesList(globalFilter, showDeleted, showOffsite));
        return ResponseEntity.ok(employeeDispatcher.getEmployeesList());
    }

    /**
     * Получает список монтажников
     * @return
     */
    @GetMapping("installers")
    public ResponseEntity<List<Employee>> getInstallersList() {
        return ResponseEntity.ok(employeeDispatcher.getInstallersList());
    }

    /**
     * Получить список сотрудников отфильтрованых по фильр-форме
     */
    @PostMapping("filter/list")
    public ResponseEntity<List<Employee>> getEmployeesListFiltered(@RequestBody EmployeeDispatcher.FiltrationForm form) {
        return ResponseEntity.ok(employeeDispatcher.getEmployeesListFiltered(form));
    }

    /**
     * Получает сотрудника по логину
     *
     * @param login
     * @return
     */
    @GetMapping("{login}")
    public ResponseEntity<Employee> getEmployee(@PathVariable String login) {
        Employee employeeByLogin;
        try {
            employeeByLogin = employeeDispatcher.getEmployee(login);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
        return ResponseEntity.ok(employeeByLogin);
    }

    /**
     * Создает сотрудника
     *
     * @param body
     * @return
     */
    @PostMapping("")
    public ResponseEntity<Employee> createEmployee(@RequestBody EmployeeForm body) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания сотрудника");
        if (body.getFirstName() == null || body.getFirstName().isBlank())
            throw new ResponseException("В запросе нет имени сотрудника");
        if (body.getLogin() == null || body.getLogin().isBlank()) throw new ResponseException("В запросе нет логина");
        if (body.getPassword() == null || body.getPassword().isBlank())
            throw new ResponseException("В запросе нет пароля");
        if (body.getDepartment() == null) throw new ResponseException("Сотруднику не присвоен отдел");
        if (body.getPosition() == null) throw new ResponseException("Сотруднику не присвоена должность");
        try {
            Employee employee = employeeDispatcher.create(body);
            stompController.createEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (AlreadyExists e) {
            throw new ResponseException("Сотрудник с данным логином уже существует");
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    /**
     * Редактирует информацию о сотруднике
     *
     * @param body
     * @param login
     * @return
     */
    @PatchMapping("{login}")
    public ResponseEntity<Employee> editEmployee(@RequestBody EmployeeForm body, @PathVariable String login) {
        if (body == null) throw new ResponseException("В запросе нет данных необходимых для создания сотрудника");
        if (body.getFirstName() == null || body.getFirstName().isBlank())
            throw new ResponseException("В запросе нет имени сотрудника");
        if (body.getPassword() == null || body.getPassword().isBlank())
            throw new ResponseException("В запросе нет пароля");
        if (body.getDepartment() == null) throw new ResponseException("Сотруднику не присвоен отдел");
        if (body.getPosition() == null) throw new ResponseException("Сотруднику не присвоена должность");
        try {
            Employee employee = employeeDispatcher.edit(body);
            stompController.updateEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (EntryNotFound e) {
            throw new ResponseException("Сотрудник с логином " + login + " не найден в базе данных для редактирования");
        } catch (EditingNotPossible e) {
            throw new ResponseException("Сотрудник удален, не возможно отредактировать");
        }
    }

    /**
     * Удаляет сотрудника
     */
    @DeleteMapping("{login}")
    public ResponseEntity<Employee> deleteEmployee(@PathVariable String login) {
        try {
            Employee employee = employeeDispatcher.delete(login);
            stompController.deleteEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (EntryNotFound e) {
            throw new ResponseException("Сотрудник с логином " + login + " не найден в базе данных");
        }
    }

    /**
     * Удаляет связь сотрудника с SIP телефоном
     *
     * @param request
     * @return
     */
    @PatchMapping("phy-phone/null/bind")
    public ResponseEntity<Void> setPhyPhoneBind(HttpServletRequest request) {
        Employee currentUser = employeeDispatcher.getEmployeeFromRequest(request);
        employeeDispatcher.setPhyPhoneBind(null, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Привязывает SIP телефон к сотруднику
     *
     * @param phoneId
     * @param request
     * @return
     */
    @PatchMapping("phy-phone/{phoneId}/bind")
    public ResponseEntity<Void> setPhyPhoneBind(@PathVariable Long phoneId, HttpServletRequest request) {
        Employee currentUser = employeeDispatcher.getEmployeeFromRequest(request);
        employeeDispatcher.setPhyPhoneBind(phyPhoneService.get(phoneId), currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Изменить статус сотрудника
     *
     * @param status
     * @param request
     * @return
     */
    @PatchMapping("status")
    public ResponseEntity<Employee> changeEmployeeStatus(@RequestBody String status, HttpServletRequest request) {
        Employee currentUser = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            Employee employee = employeeDispatcher.changeStatus(currentUser.getLogin(), EmployeeStatus.valueOf(status));
            stompController.updateEmployee(employee);
            return ResponseEntity.ok(employee);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

    /**
     * Сохраняет настройки уведомлений
     */
    @PatchMapping("notification-settings")
    public ResponseEntity<Employee> saveNotificationSettings(@RequestBody NotificationSettings.Form form, HttpServletRequest request) {
        Employee targetUser = employeeDispatcher.getEmployeeFromRequest(request);
        try {
            if (targetUser.getNotificationSettings() == null){
                targetUser.setNotificationSettings(NotificationSettings.from(form));
            } else {
                targetUser.getNotificationSettings().update(form);
            }
            stompController.updateEmployee(targetUser);
            return ResponseEntity.ok(employeeDispatcher.unsafeSave(targetUser));
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }
    }

}
