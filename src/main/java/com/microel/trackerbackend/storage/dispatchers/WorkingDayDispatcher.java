package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.SalaryTable;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.repositories.WorkingDayRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class WorkingDayDispatcher {
    private final WorkingDayRepository workingDayRepository;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;

    public WorkingDayDispatcher(WorkingDayRepository workingDayRepository, EmployeeDispatcher employeeDispatcher, StompController stompController) {
        this.workingDayRepository = workingDayRepository;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
    }

    public void addCalculation(String login, Date date, WorkCalculation calculation) {
        List<WorkingDay> all = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.join("employee").get("login"), login));
            predicates.add(cb.equal(root.get("date"), date));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        if (all.size() == 0) {
            Employee employee = employeeDispatcher.getEmployee(login);
            WorkingDay workingDay = WorkingDay.builder()
                    .employee(employee)
                    .date(date)
                    .calculations(Stream.of(calculation).collect(Collectors.toList()))
                    .build();
            stompController.createWorkingDay(workingDayRepository.save(workingDay));
        } else {
            all.get(0).getCalculations().add(calculation);
            stompController.updateWorkingDay(workingDayRepository.save(all.get(0)));
        }
    }

    public SalaryTable getTableByDate(@Nullable Date date, @Nullable Long position) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date start = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date end = calendar.getTime();


        Map<Employee, List<WorkingDay>> rows = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("date"), start, end));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().collect(Collectors.groupingBy(WorkingDay::getEmployee));

        List<Employee> employeesByPosition = employeeDispatcher.getByPosition(position);

        SalaryTable salaryTable = new SalaryTable();

        salaryTable.setHeaders(start);
        salaryTable.setEmployees(employeesByPosition);

        for (Employee employee : employeesByPosition) {
            Calendar wdCalendar = GregorianCalendar.getInstance();
            List<SalaryTable.SalaryTableCell> row = new ArrayList<>();
            wdCalendar.setTime(start);
            if (rows.containsKey(employee)) {
                List<WorkingDay> workingDays = rows.get(employee);
                for (int i = 1; i <= end.getDate(); i++) {
                    wdCalendar.set(Calendar.DAY_OF_MONTH, i);
                    Integer targetDate = wdCalendar.getTime().getDate();
                    SalaryTable.SalaryTableCell cell = workingDays.stream()
                            .filter(wd -> targetDate.equals(wd.getDate().getDate()))
                            .map(WorkingDay::toPoint)
                            .findFirst().orElse(new SalaryTable.SalaryTableCell(wdCalendar.getTime(), employee));
                    row.add(cell);
                }
            } else {
                for (int i = 1; i <= end.getDate(); i++) {
                    wdCalendar.set(Calendar.DAY_OF_MONTH, i);
                    Date targetDate = wdCalendar.getTime();
                    row.add(new SalaryTable.SalaryTableCell(targetDate, employee));
                }
            }
            salaryTable.addRow(row);
        }

        return salaryTable;
    }


    @Nullable
    public WorkingDay getWorkingDay(Date date, String login) {
        return workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.join("employee").get("login"), login));
            predicates.add(cb.equal(root.get("date"), date));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().findFirst().orElse(null);
    }
}
