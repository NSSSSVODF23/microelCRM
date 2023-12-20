package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.BypassWorkCalculationForm;
import com.microel.trackerbackend.misc.FactorAction;
import com.microel.trackerbackend.misc.ResponseWorkEstimationForm;
import com.microel.trackerbackend.misc.WorkCalculationForm;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.salary.ActionTaken;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.WorkCalculationRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final TaskDispatcher taskDispatcher;

    public WorkCalculationDispatcher(WorkCalculationRepository workCalculationRepository, ActionTakenDispatcher actionTakenDispatcher, EmployeeDispatcher employeeDispatcher, WorkLogDispatcher workLogDispatcher, PaidActionDispatcher paidActionDispatcher, PaidWorkDispatcher paidWorkDispatcher, StompController stompController, WorkingDayDispatcher workingDayDispatcher, TaskDispatcher taskDispatcher) {
        this.workCalculationRepository = workCalculationRepository;
        this.actionTakenDispatcher = actionTakenDispatcher;
        this.employeeDispatcher = employeeDispatcher;
        this.workLogDispatcher = workLogDispatcher;
        this.paidActionDispatcher = paidActionDispatcher;
        this.paidWorkDispatcher = paidWorkDispatcher;
        this.stompController = stompController;
        this.workingDayDispatcher = workingDayDispatcher;
        this.taskDispatcher = taskDispatcher;
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

        calculating(workLog, form.getActions(), form.getSpreading(), form.getEmptyDescription(), creator, form.getEditingDescription(), form.getIsPaidWork(), form.getAmountOfMoneyTaken());
    }

    private void calculating(WorkLog workLog, List<WorkCalculationForm.ActionCalculationItem> formActions,
                             List<WorkCalculationForm.SpreadingItem> formSpreading, String emptyCalcDescription,
                             Employee creator, @Nullable String editingDescription, Boolean isPaidWork, @Nullable Float amountOfMoneyTaken) {
        List<WorkCalculation> workCalculationList = null;
        if (workLog.getCalculated()) {
            workCalculationList = workCalculationRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                Join<WorkLog, WorkCalculation> workLogJoin = root.join("workLog");
                predicates.add(cb.equal(workLogJoin.get("workLogId"), workLog.getWorkLogId()));
                return cb.and(predicates.toArray(Predicate[]::new));
            });
        }

        List<ActionTaken> actions = new ArrayList<>();
        Boolean empty = false;
        if (formActions == null || formActions.isEmpty()) {
            if (emptyCalcDescription == null || emptyCalcDescription.isBlank()) {
                throw new IllegalFields("Нужно указать причину пустого подсчета");
            }
            empty = true;
        } else {
            for (WorkCalculationForm.ActionCalculationItem item : formActions) {

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
                        .work(paidWork)
                        .paidAction(paidAction)
                        .count(item.getCount())
                        .uuid(item.getUuid())
                        .build());
            }
        }


        List<ActionTaken> savedActions = actionTakenDispatcher.saveAll(actions);

        if (workCalculationList != null && !workCalculationList.isEmpty()) {
            if (editingDescription == null || editingDescription.isBlank()) {
                throw new IllegalFields("Нужно указать причину пересчета");
            }
            for (WorkCalculation workCalculation : workCalculationList) {

                workCalculation.setActions(savedActions);

                for (Employee employee : workLog.getEmployees()) {
                    if (workCalculation.getEmployee().equals(employee)) {
                        WorkCalculationForm.SpreadingItem currentEmployeeSpreading = formSpreading.stream()
                                .filter(spreadingItem -> spreadingItem.getLogin().equals(employee.getLogin()))
                                .findFirst().orElseThrow(() -> new IllegalFields("Сотрудник из журнала данной работы не найден в списке подсчета"));
                        Float currentRatio = currentEmployeeSpreading.getRatio();
                        List<FactorAction> currentFactorsActions = currentEmployeeSpreading.getFactorsActions();
                        workCalculation.setRatio(currentRatio);
                        workCalculation.setFactorsActions(currentFactorsActions);
                        workCalculation.setEmpty(empty);
                        workCalculation.setEmptyDescription(emptyCalcDescription);
                        workCalculation.addEditedBy(creator, editingDescription);
                        workCalculation.setIsPaidWork(isPaidWork != null && isPaidWork);
                        workCalculation.setAmountOfMoneyTaken(amountOfMoneyTaken);
                        workCalculationRepository.save(workCalculation);
                    }
                }
            }
            return;
        }

        List<WorkCalculation> savedCalculation = new ArrayList<>();
        for (Employee employee : workLog.getEmployees()) {
            WorkCalculationForm.SpreadingItem currentEmployeeSpreading = formSpreading.stream()
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
                    .emptyDescription(emptyCalcDescription)
                    .isPaidWork(isPaidWork != null && isPaidWork)
                    .amountOfMoneyTaken(amountOfMoneyTaken)
                    .build();
            WorkCalculation saved = workCalculationRepository.save(workCalculation);
            savedCalculation.add(saved);
            workingDayDispatcher.addCalculation(employee.getLogin(), Utils.trimDate(Date.from(workLog.getCreated().toInstant())), saved);
        }

        workLog.setCalculated(true);
        workLog.setWorkCalculations(savedCalculation);

        workLogDispatcher.save(workLog);
        stompController.updateWorkLog(workLog);
        stompController.updateSalaryTable(workingDayDispatcher.getTableByEmployees(workLog.getCreated(), workLog.getEmployees()));
    }

    @Nullable
    public ResponseWorkEstimationForm getFormInfoByWorkLog(Long workLogId) {
        List<WorkCalculation> workCalculations = workCalculationRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<WorkLog, WorkCalculation> workLogJoin = root.join("workLog", JoinType.LEFT);
            predicates.add(cb.equal(workLogJoin.get("workLogId"), workLogId));
            return cb.and(predicates.toArray(Predicate[]::new));
        });

        if (workCalculations.isEmpty()) return null;

        Map<String, ResponseWorkEstimationForm.EmployeeRatioValue> ratioValueMap = new HashMap<>();
        for (WorkCalculation workCalculation : workCalculations) {
            ratioValueMap.put(workCalculation.getEmployee().getLogin(), ResponseWorkEstimationForm.EmployeeRatioValue.builder()
                    .ratio(workCalculation.getRatio())
                    .sum(workCalculation.getSum(true))
                    .build());
        }

        ResponseWorkEstimationForm form = ResponseWorkEstimationForm.builder()
                .actions(workCalculations.get(0).getActions().stream().map(ActionTaken::toFormItem).collect(Collectors.toList()))
                .factorsActions(workCalculations.stream().map(WorkCalculation::getFactorActionsFormItems).flatMap(List::stream).collect(Collectors.toList()))
                .employeesRatio(ratioValueMap)
                .isPaidWork(workCalculations.get(0).getIsPaidWork())
                .amountOfMoneyTaken(workCalculations.get(0).getAmountOfMoneyTaken())
                .build();

        return form;
    }


    public void calculateAndSaveBypass(BypassWorkCalculationForm form, Employee employee) {
        Task task = taskDispatcher.createTask(form.getTaskInfo(), form.getReportInfo().getDate(), employee);
        stompController.createTask(task);
        stompController.updateTask(taskDispatcher.modifyTags(task.getTaskId(), form.getReportInfo().getTags()));
        WorkLog workLog = workLogDispatcher.createWorkLog(task, form.getReportInfo(), form.getReportInfo().getDate(), employee);
        stompController.createWorkLog(workLog);
        workLog.getChat().setClosed(form.getReportInfo().getDate());
        calculating(workLog, form.getActions(), form.getSpreading(), null, employee, null, form.getIsPaidWork(), form.getAmountOfMoneyTaken());
        stompController.closeWorkLog(workLog);
        stompController.closeChat(workLog.getChat());
        stompController.updateTask(taskDispatcher.close(task.getTaskId(), employee));
    }
}
