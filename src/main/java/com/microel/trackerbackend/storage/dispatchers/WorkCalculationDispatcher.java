package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.WorkCalculationForm;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.salary.ActionTaken;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.WorkCalculationRepository;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class WorkCalculationDispatcher {
    private final WorkCalculationRepository workCalculationRepository;
    private final ActionTakenDispatcher actionTakenDispatcher;
    private final EmployeeDispatcher employeeDispatcher;
    private final WorkLogDispatcher workLogDispatcher;
    private final PaidActionDispatcher paidActionDispatcher;
    private final PaidWorkDispatcher paidWorkDispatcher;
    private final StompController stompController;
    private final WorkingDayDispatcher workingDayDispatcher;

    public WorkCalculationDispatcher(WorkCalculationRepository workCalculationRepository, ActionTakenDispatcher actionTakenDispatcher, EmployeeDispatcher employeeDispatcher, WorkLogDispatcher workLogDispatcher, PaidActionDispatcher paidActionDispatcher, PaidWorkDispatcher paidWorkDispatcher, StompController stompController, WorkingDayDispatcher workingDayDispatcher) {
        this.workCalculationRepository = workCalculationRepository;
        this.actionTakenDispatcher = actionTakenDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.paidActionDispatcher = paidActionDispatcher;
        this.paidWorkDispatcher = paidWorkDispatcher;
        this.stompController = stompController;
        this.workingDayDispatcher = workingDayDispatcher;
    }

    public void calculateAndSave(WorkCalculationForm form, Employee creator) {
        if (form.getWorkLogId() == null) throw new IllegalFields("Не указан id журнала работ");
        if (form.getSpreading() == null || form.getSpreading().isEmpty()) {
            throw new IllegalFields("Нет объектов распределения");
        } else {
            boolean emptyLogin = form.getSpreading().stream().anyMatch(spreadingItem -> spreadingItem.getLogin() == null || spreadingItem.getLogin().isBlank());
            boolean wrongRatio = form.getSpreading().stream().anyMatch(spreadingItem -> spreadingItem.getRatio() == null || spreadingItem.getRatio() < 0 || spreadingItem.getRatio() > 1);
            boolean tooBigRatio = form.getSpreading().stream().reduce(0F, (part, spreadingItem) -> part + spreadingItem.getRatio(), Float::sum) > 1;
            if (emptyLogin) {
                throw new IllegalFields("Не указан логин сотрудника для распределения");
            } else if (wrongRatio) {
                throw new IllegalFields("Соотношение распределения должно быть в диапазоне от 0 до 1");
            } else if (tooBigRatio) {
                throw new IllegalFields("Сумма соотношений распределения должна быть меньше или равна 1");
            }
        }
        WorkLog workLog = workLogDispatcher.get(form.getWorkLogId());
        if (workLog.getCalculated()) throw new IllegalFields("Этот журнал работ уже был рассчитан");

        List<ActionTaken> actions = new ArrayList<>();
        Boolean empty = false;
        if (form.getActions() == null || form.getActions().isEmpty()) {
            if (form.getEmptyDescription() == null || form.getEmptyDescription().isBlank()) {
                throw new IllegalFields("Нужно указать причину пустого подсчета");
            }
            empty = true;
        } else {
            for (WorkCalculationForm.ActionCalculationItem item : form.getActions()) {

                if (item.getCount() == null || item.getCount() <= 0) {
                    throw new IllegalFields("Количество платных действий может быть положительным числом");
                }

                if (item.getUuid() == null) {
                    throw new IllegalFields("Не указан uuid платного действия");
                }

                PaidWork paidWork = null;
                if (item.getWorkId() != null) {
                    paidWork = paidWorkDispatcher.get(item.getWorkId());
                }

                PaidAction paidAction = paidActionDispatcher.get(item.getActionId());

                actions.add(ActionTaken.builder()
                        .workName(paidWork != null ? paidWork.getName() : null)
                        .paidAction(paidAction)
                        .count(item.getCount())
                        .uuid(item.getUuid())
                        .build());
            }
        }

        List<ActionTaken> savedActions = actionTakenDispatcher.saveAll(actions);

        List<WorkCalculation> savedCalculation = new ArrayList<>();
        for (Employee employee : workLog.getEmployees()) {
            WorkCalculationForm.SpreadingItem currentEmployeeSpreading = form.getSpreading().stream()
                    .filter(spreadingItem -> spreadingItem.getLogin().equals(employee.getLogin()))
                    .findFirst().orElseThrow(() -> new IllegalFields("Сотрудник из журнала данной работы не найден в списке подсчета"));
            Float currentRatio = currentEmployeeSpreading.getRatio();
            WorkCalculation workCalculation = WorkCalculation.builder()
                    .workLog(workLog)
                    .actions(savedActions)
                    .employee(employee)
                    .ratio(currentRatio)
                    .factorsActions(currentEmployeeSpreading.getFactorsActions())
                    .created(Timestamp.from(Instant.now()))
                    .creator(creator)
                    .empty(empty)
                    .emptyDescription(form.getEmptyDescription())
                    .build();
            WorkCalculation saved = workCalculationRepository.save(workCalculation);
            savedCalculation.add(saved);
            workingDayDispatcher.addCalculation(employee.getLogin(), Date.from(workLog.getCreated().toInstant()), saved);
        }

        workLog.setCalculated(true);
        workLog.setWorkCalculations(savedCalculation);

        workLogDispatcher.save(workLog);
        stompController.updateWorkLog(workLog);
    }


}
