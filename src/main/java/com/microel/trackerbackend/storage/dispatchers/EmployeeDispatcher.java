package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.security.PasswordService;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.entities.team.util.EmployeeStatus;
import com.microel.trackerbackend.storage.entities.team.util.Position;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EditingNotPossible;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class EmployeeDispatcher {
    public final EmployeeRepository employeeRepository;
    private final DepartmentDispatcher departmentDispatcher;
    private final PositionDispatcher positionDispatcher;
    private final PasswordService passwordService;
    private final TelegramController telegramController;

    public EmployeeDispatcher(EmployeeRepository employeeRepository,
                              DepartmentDispatcher departmentDispatcher, PositionDispatcher positionDispatcher, PasswordService passwordService, @Lazy TelegramController telegramController) {
        this.employeeRepository = employeeRepository;
        this.departmentDispatcher = departmentDispatcher;
        this.positionDispatcher = positionDispatcher;
        this.passwordService = passwordService;
        this.telegramController = telegramController;

        if (employeeRepository.count() == 0) {
            try {
                create("", "", "", "admin", "admin", 0, "", "", null, null, false);
            } catch (AlreadyExists | EntryNotFound e) {
                log.warn("Не удалось создать запись администратора");
            }
        }
    }

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

    public Employee create(
            String firstName,
            String lastName,
            String secondName,
            String login,
            String password,
            Integer access,
            String internalPhoneNumber,
            String telegramUserId,
            @Nullable
            Long department,
            @Nullable
            Long position,
            Boolean offsite
    ) throws AlreadyExists, EntryNotFound {
        boolean exists = employeeRepository.existsById(login);
        if (exists) throw new AlreadyExists();
        Employee foundEmployeeTelegramId = employeeRepository.findTopByTelegramUserId(telegramUserId).orElse(null);
        if (
                (telegramUserId != null && !telegramUserId.isBlank())
                        && foundEmployeeTelegramId != null
                        && !Objects.equals(foundEmployeeTelegramId.getLogin(), login)
        )
            throw new ResponseException("Уже есть сотрудник с данным Telegram ID");

        try {
            telegramController.sendMessageToTlgId(telegramUserId, "Ваш аккаунт в Telegram привязан к учетной записи Microel");
        } catch (Throwable e) {
            throw new ResponseException("Не корректный Telegram ID");
        }

        Department foundDepartment = departmentDispatcher.getById(department);

        Position foundPosition = positionDispatcher.getById(position);

        Employee.EmployeeBuilder employeeBuilder = Employee.builder();
        employeeBuilder
                .firstName(firstName)
                .lastName(lastName)
                .secondName(secondName)
                .login(login)
                .password(passwordService.encryptPassword(password))
                .access(access)
                .internalPhoneNumber(internalPhoneNumber)
                .telegramUserId(telegramUserId)
                .department(foundDepartment)
                .position(foundPosition)
                .created(Timestamp.from(Instant.now()))
                .deleted(false);

        if (offsite == null) employeeBuilder.offsite(false);
        else employeeBuilder.offsite(offsite);

        return employeeRepository.save(employeeBuilder.build());
    }

    public Employee edit(
            String firstName,
            String lastName,
            String secondName,
            String login,
            String password,
            Integer access,
            String internalPhoneNumber,
            String telegramUserId,
            Long department,
            Long position,
            Boolean offsite
    ) throws EntryNotFound, EditingNotPossible {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        Employee foundEmployeeTelegramId = employeeRepository.findTopByTelegramUserId(telegramUserId).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound();
        if (foundEmployee.getDeleted()) throw new EditingNotPossible();
        if (
                (telegramUserId != null && !telegramUserId.isBlank())
                        && foundEmployeeTelegramId != null
                        && !Objects.equals(foundEmployeeTelegramId.getLogin(), login)
        )
            throw new ResponseException("Уже есть сотрудник с данным Telegram ID");

        if (foundEmployeeTelegramId == null || (telegramUserId != null && !telegramUserId.isBlank() && !Objects.equals(foundEmployeeTelegramId.getTelegramUserId(), telegramUserId))) {
            try {
                telegramController.sendMessageToTlgId(telegramUserId, "Ваш аккаунт в Telegram привязан к учетной записи Microel");
            } catch (Throwable e) {
                throw new ResponseException("Не корректный Telegram ID");
            }
        }

        Department foundDepartment = departmentDispatcher.getById(department);
        Position foundPosition = positionDispatcher.getById(position);

        foundEmployee.setFirstName(firstName);
        foundEmployee.setLastName(lastName);
        foundEmployee.setSecondName(secondName);
        if (!password.equals("password")) foundEmployee.setPassword(passwordService.encryptPassword(password));
        foundEmployee.setAccess(access);
        foundEmployee.setInternalPhoneNumber(internalPhoneNumber);
        foundEmployee.setTelegramUserId(telegramUserId);
        foundEmployee.setDepartment(foundDepartment);
        foundEmployee.setPosition(foundPosition);
        foundEmployee.setOffsite(offsite);

        return employeeRepository.save(foundEmployee);
    }

    public Employee delete(String login) throws EntryNotFound {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound();
        foundEmployee.setDeleted(true);
        return employeeRepository.save(foundEmployee);
    }

    public Employee getEmployee(String login) throws EntryNotFound {
        Employee employee = employeeRepository.findById(login).orElse(null);
        if (employee == null) throw new EntryNotFound("Сотрудника с логином " + login + " не существует");
        return employee;
    }

    public boolean isExist(String login) {
        return employeeRepository.existsByLoginAndDeletedEquals(login, false);
    }

    public List<Employee> getInstallersList() {
        return employeeRepository.findAllByOffsiteIsTrue();
    }

    public List<Employee> getByIdSet(Set<String> personalResponsibilities) {
        return employeeRepository.findAllByLoginIsInAndDeletedIsFalseAndOffsiteIsFalse(personalResponsibilities);
    }

    public Employee setOnline(String login) throws EntryNotFound {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound("Сотрудник не найден");
        foundEmployee.setStatus(EmployeeStatus.ONLINE);
        foundEmployee.setLastSeen(Timestamp.from(Instant.now()));
        return employeeRepository.save(foundEmployee);
    }

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

    public Employee changeStatus(String login, EmployeeStatus status) throws EntryNotFound {
        Employee foundEmployee = employeeRepository.findById(login).orElse(null);
        if (foundEmployee == null) throw new EntryNotFound("Сотрудник не найден");
        foundEmployee.setStatus(status);
        return employeeRepository.save(foundEmployee);
    }

    // Если сотрудник не существует, сохраняет его в базе данных и возвращает true
    public Boolean saveIsNotExist(Employee employee) {
        if (employeeRepository.existsById(employee.getLogin())) return false;
        employeeRepository.save(employee);
        return true;
    }

    public Optional<Employee> getByTelegramId(Long chatId) {
        return employeeRepository.findTopByTelegramUserId(chatId.toString());
    }

    public List<Employee> getByPosition(Long position) {
        return employeeRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.join("position", JoinType.LEFT).get("positionId"), position));
            predicates.add(cb.isFalse(root.get("deleted")));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }
}
