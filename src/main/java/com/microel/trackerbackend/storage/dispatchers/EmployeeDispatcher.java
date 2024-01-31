package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.misc.ListItem;
import com.microel.trackerbackend.security.AuthorizationProvider;
import com.microel.trackerbackend.security.PasswordService;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.*;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EditingNotPossible;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Component
@Slf4j
@Transactional(readOnly = true)
public class EmployeeDispatcher {
    public final EmployeeRepository employeeRepository;
    private final DepartmentDispatcher departmentDispatcher;
    private final PositionDispatcher positionDispatcher;
    private final PasswordService passwordService;
    private final TelegramController telegramController;
    private final StompController stompController;

    public EmployeeDispatcher(EmployeeRepository employeeRepository,
                              DepartmentDispatcher departmentDispatcher, PositionDispatcher positionDispatcher,
                              PasswordService passwordService, @Lazy TelegramController telegramController,
                              @Lazy StompController stompController) {
        this.employeeRepository = employeeRepository;
        this.departmentDispatcher = departmentDispatcher;
        this.positionDispatcher = positionDispatcher;
        this.passwordService = passwordService;
        this.telegramController = telegramController;
        this.stompController = stompController;

//        if (employeeRepository.count() == 0) {
//            try {
//                create("", "", "", "admin", "admin", 0, "", "", null, null, false, null);
//            } catch (AlreadyExists | EntryNotFound e) {
//                log.warn("Не удалось создать запись администратора");
//            }
//        }
    }

    @Transactional
    public Employee unsafeSave(Employee employee) {
        return employeeRepository.save(employee);
    }

    public List<Employee> getEmployeesList() {
        return employeeRepository.findAll(Sort.by(Sort.Direction.ASC, "secondName", "firstName", "lastName", "login"));
    }

    public List<Employee> getEmployeesList(@Nullable String globalFilter, @Nullable Boolean isDeleted, @Nullable Boolean isOffsite) {
        return employeeRepository.findAll((root, query, cb) -> {
            List<Predicate> globalSearchPredicates = new ArrayList<>();
            List<Predicate> flagsPredicates = new ArrayList<>();
            if (globalFilter != null && !globalFilter.isBlank()) {
                globalSearchPredicates.add(cb.like(cb.lower(root.get("secondName")), "%" + globalFilter.toLowerCase() + "%"));
                globalSearchPredicates.add(cb.like(cb.lower(root.get("firstName")), "%" + globalFilter.toLowerCase() + "%"));
                globalSearchPredicates.add(cb.like(cb.lower(root.get("lastName")), "%" + globalFilter.toLowerCase() + "%"));
                globalSearchPredicates.add(cb.like(cb.lower(root.get("login")), "%" + globalFilter.toLowerCase() + "%"));
                globalSearchPredicates.add(cb.like(cb.lower(root.join("department", JoinType.LEFT).get("name")), "%" + globalFilter.toLowerCase() + "%"));
                globalSearchPredicates.add(cb.like(cb.lower(root.join("position", JoinType.LEFT).get("name")), "%" + globalFilter.toLowerCase() + "%"));
            }
            if (isDeleted != null) flagsPredicates.add(cb.equal(root.get("deleted"), isDeleted));
            if (isOffsite != null) flagsPredicates.add(cb.equal(root.get("offsite"), isOffsite));
            if (!globalSearchPredicates.isEmpty()) {
                return cb.and(cb.or(globalSearchPredicates.toArray(Predicate[]::new)), cb.and(flagsPredicates.toArray(Predicate[]::new)));
            } else {
                return cb.and(flagsPredicates.toArray(Predicate[]::new));
            }
        }, Sort.by(Sort.Direction.ASC, "secondName", "firstName", "lastName", "login"));
    }

    @Transactional
    public Employee create(EmployeeForm form) throws AlreadyExists, EntryNotFound {
        boolean exists = employeeRepository.existsById(form.getLogin());
        if (exists) throw new AlreadyExists();
        Employee foundEmployeeTelegramId = null;
        if(form.getTelegramUserId() != null && !form.getTelegramUserId().isBlank())
            foundEmployeeTelegramId = employeeRepository.findTopByTelegramUserId(form.getTelegramUserId()).orElse(null);
        if (foundEmployeeTelegramId != null)
            throw new ResponseException("Уже есть сотрудник с данным Telegram ID");

        if(form.getTelegramUserId() != null && !form.getTelegramUserId().isBlank()) {
            try {
                telegramController.sendMessageToTlgId(form.getTelegramUserId(), "Ваш аккаунт в Telegram привязан к учетной записи Microel");
            } catch (Throwable e) {
                throw new ResponseException("Не корректный Telegram ID");
            }
        }

        Department foundDepartment = departmentDispatcher.getById(form.getDepartment());

        Position foundPosition = positionDispatcher.getById(form.getPosition());

        return employeeRepository.save(form.toNewEmployee(foundDepartment, foundPosition, passwordService));
    }

    @Transactional
    public Employee edit(EmployeeForm form) throws EntryNotFound, EditingNotPossible {
        Employee foundEmployee = employeeRepository.findById(form.getLogin()).orElse(null);
        Employee foundEmployeeTelegramId = null;
        if(form.getTelegramUserId() != null && !form.getTelegramUserId().isBlank())
            foundEmployeeTelegramId = employeeRepository.findTopByTelegramUserId(form.getTelegramUserId()).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound();
        if (foundEmployee.getDeleted()) throw new EditingNotPossible();
        if (foundEmployeeTelegramId != null && !Objects.equals(foundEmployeeTelegramId.getLogin(), form.getLogin()))
            throw new ResponseException("Уже есть сотрудник с данным Telegram ID");

        if ((form.getTelegramUserId() != null && !form.getTelegramUserId().isBlank()) && (foundEmployeeTelegramId == null || !Objects.equals(foundEmployeeTelegramId.getTelegramUserId(), form.getTelegramUserId()))) {
            try {
                telegramController.sendMessageToTlgId(form.getTelegramUserId(), "Ваш аккаунт в Telegram привязан к учетной записи Microel");
            } catch (Throwable e) {
                throw new ResponseException("Не корректный Telegram ID");
            }
        }

        if(form.getTelegramGroupChatId() != null && !form.getTelegramGroupChatId().isBlank() && !Objects.equals(foundEmployee.getTelegramGroupChatId(), form.getTelegramGroupChatId())){
            try {
                telegramController.sendMessageToTlgId(form.getTelegramGroupChatId(), foundEmployee.getFullName() + " назначена рабочая группа");
            } catch (Throwable e) {
                throw new ResponseException("Не корректный id группового рабочего чата");
            }
        }

        Department foundDepartment = departmentDispatcher.getById(form.getDepartment());
        Position foundPosition = positionDispatcher.getById(form.getPosition());

        return employeeRepository.save(form.updateEmployee(foundEmployee, foundDepartment, foundPosition, passwordService));
    }

    @Transactional
    public Employee delete(String login) throws EntryNotFound {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound();
        foundEmployee.setDeleted(true);
        foundEmployee.setTelegramUserId("");
        return employeeRepository.save(foundEmployee);
    }

    public Employee getEmployee(String login) throws EntryNotFound {
        Employee employee = employeeRepository.findById(login).orElse(null);
        if (employee == null) throw new EntryNotFound("Сотрудника с логином " + login + " не существует");
        return employee;
    }

    public Employee getEmployeeFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) throw new ResponseException("Не авторизован");
        String login = AuthorizationProvider.getLoginFromCookie(List.of(request.getCookies()));
        if (login == null) throw new ResponseException("Не удалось получить данные о текущем пользователе");

        Employee currentUser;
        try {
            currentUser = getEmployee(login);
        } catch (EntryNotFound e) {
            throw new ResponseException(e.getMessage());
        }

        if (currentUser.getDeleted()) throw new ResponseException("Сотрудник удален");

        return currentUser;
    }

    public boolean isExist(String login) {
        return employeeRepository.existsByLoginAndDeletedEquals(login, false);
    }

    public List<Employee> getInstallersList() {
        return employeeRepository.findAllByOffsiteIsTrueAndDeletedIsFalse();
    }

    public List<Employee> getByIdSet(Set<String> personalResponsibilities) {
        return employeeRepository.findAllByLoginIsInAndDeletedIsFalseAndOffsiteIsFalse(personalResponsibilities);
    }

    @Transactional
    public Employee setOnline(String login) throws EntryNotFound {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound("Сотрудник не найден");
        foundEmployee.setStatus(EmployeeStatus.ONLINE);
        foundEmployee.setLastSeen(Timestamp.from(Instant.now()));
        return employeeRepository.save(foundEmployee);
    }

    @Transactional
    public Employee setOffline(String login) throws EntryNotFound {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound("Сотрудник не найден");
        foundEmployee.setStatus(EmployeeStatus.OFFLINE);
        foundEmployee.setLastSeen(Timestamp.from(Instant.now()));
        return employeeRepository.save(foundEmployee);
    }

    public Set<Employee> getValidEmployees(List<String> logins) {
        return employeeRepository.findAllByLoginInAndDeletedIsFalseAndOffsiteIsFalse(logins);
    }

    @Transactional
    public Employee changeStatus(String login, EmployeeStatus status) throws EntryNotFound {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound("Сотрудник не найден");
        foundEmployee.setStatus(status);
        return employeeRepository.save(foundEmployee);
    }

    // Если сотрудник не существует, сохраняет его в базе данных и возвращает true
    @Transactional
    public Boolean saveIsNotExist(Employee employee) {
        if (employeeRepository.existsById(employee.getLogin())) return false;
        employeeRepository.save(employee);
        return true;
    }

    public Optional<Employee> getByTelegramId(Long chatId) {
        return employeeRepository.findTopByTelegramUserId(chatId.toString());
    }

    /**
     * Возвращает список сотрудников (монтажников) из группового чата
     * @param chatId
     * @return
     */
    public List<Employee> getByGroupTelegramId(Long chatId) {
        return employeeRepository.findAll((root,query,cb)->cb.and(cb.equal(root.get("telegramGroupChatId"), chatId.toString()), cb.isTrue(root.get("offsite"))));
    }

    public List<Employee> getByPosition(Long position) {
        return employeeRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.join("position", JoinType.LEFT).get("positionId"), position));
            predicates.add(cb.isFalse(root.get("deleted")));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    @Transactional
    public void createPhyPhoneBind(PhyPhoneInfo.Form form) {
        Employee employee = employeeRepository.findById(form.getEmployeeLogin()).orElseThrow(()->new ResponseException("Пользователь не найден"));
        PhyPhoneInfo phoneInfo = PhyPhoneInfo.from(form);
        phoneInfo.throwIfIncomplete();
        employee.setPhyPhoneInfo(phoneInfo);
        stompController.updateEmployee(employeeRepository.save(employee));
    }

    @Transactional
    public void removePhyPhoneBind(String employeeLogin) {
        Employee employee = employeeRepository.findById(employeeLogin).orElseThrow(()->new ResponseException("Пользователь не найден"));
        employee.setPhyPhoneInfo(null);
        stompController.updateEmployee(employeeRepository.save(employee));
    }

    @Transactional
    public void setPhyPhoneBind(@Nullable PhyPhoneInfo phone, Employee employee) {
        employee.setPhyPhoneInfo(phone);
        stompController.updateEmployee(employeeRepository.save(employee));
    }

    public List<Employee> getEmployeesByPositionId(Long positionId) {
        return employeeRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Employee, Position> positionJoin = root.join("position", JoinType.LEFT);
            predicates.add(cb.equal(positionJoin.get("positionId"), positionId));
            predicates.add(cb.isFalse(root.get("deleted")));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }
}
