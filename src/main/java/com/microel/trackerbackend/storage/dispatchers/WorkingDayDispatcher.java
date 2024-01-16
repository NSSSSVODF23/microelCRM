package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.SalaryTable;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.salary.WorkingDay;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.repositories.WorkingDayRepository;
import org.javatuples.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Transactional(readOnly = true)
public class WorkingDayDispatcher {
    private final WorkingDayRepository workingDayRepository;
    private final EmployeeDispatcher employeeDispatcher;
    private final StompController stompController;

    public WorkingDayDispatcher(WorkingDayRepository workingDayRepository, EmployeeDispatcher employeeDispatcher, StompController stompController) {
        this.workingDayRepository = workingDayRepository;
        this.employeeDispatcher = employeeDispatcher;
        this.stompController = stompController;
    }

    @Transactional
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
        Pair<Date, Date> monthBoundaries = Utils.getMonthBoundaries(date);


        Map<Employee, List<WorkingDay>> rows = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("date"), monthBoundaries.getValue0(), monthBoundaries.getValue1()));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().collect(Collectors.groupingBy(WorkingDay::getEmployee));

        List<Employee> employeesByPosition = position == null ? employeeDispatcher.getEmployeesList(null, false, null) : employeeDispatcher.getByPosition(position);

        SalaryTable salaryTable = new SalaryTable();

        salaryTable.setHeaders(monthBoundaries.getValue0());
        salaryTable.setEmployees(employeesByPosition);

        for (Employee employee : employeesByPosition) {
            Calendar wdCalendar = GregorianCalendar.getInstance();
            List<SalaryTable.SalaryTableCell> row = new ArrayList<>();
            wdCalendar.setTime(monthBoundaries.getValue0());
            if (rows.containsKey(employee)) {
                List<WorkingDay> workingDays = rows.get(employee);
                for (int i = 1; i <= monthBoundaries.getValue1().getDate(); i++) {
                    wdCalendar.set(Calendar.DAY_OF_MONTH, i);
                    Integer targetDate = wdCalendar.getTime().getDate();
                    SalaryTable.SalaryTableCell cell = workingDays.stream()
                            .filter(wd -> targetDate.equals(wd.getDate().getDate()))
                            .map(WorkingDay::toPoint)
                            .findFirst().orElse(new SalaryTable.SalaryTableCell(wdCalendar.getTime(), employee));
                    row.add(cell);
                }
            } else {
                for (int i = 1; i <= monthBoundaries.getValue1().getDate(); i++) {
                    wdCalendar.set(Calendar.DAY_OF_MONTH, i);
                    Date targetDate = wdCalendar.getTime();
                    row.add(new SalaryTable.SalaryTableCell(targetDate, employee));
                }
            }
            salaryTable.addRow(row);
        }

        return salaryTable;
    }

    public SalaryTable getTableByEmployees(Date date, Set<Employee> employees) {
        Pair<Date, Date> monthBoundaries = Utils.getMonthBoundaries(date);

        Map<Employee, List<WorkingDay>> rows = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("employee").in(employees));
            predicates.add(cb.between(root.get("date"), monthBoundaries.getValue0(), monthBoundaries.getValue1()));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().collect(Collectors.groupingBy(WorkingDay::getEmployee));

        SalaryTable salaryTable = new SalaryTable();

        salaryTable.setHeaders(monthBoundaries.getValue0());
        salaryTable.setEmployees(rows.keySet().stream().toList());

        for (Employee employee : rows.keySet()) {
            Calendar wdCalendar = GregorianCalendar.getInstance();
            List<SalaryTable.SalaryTableCell> row = new ArrayList<>();
            wdCalendar.setTime(monthBoundaries.getValue0());
            if (rows.containsKey(employee)) {
                List<WorkingDay> workingDays = rows.get(employee);
                for (int i = 1; i <= monthBoundaries.getValue1().getDate(); i++) {
                    wdCalendar.set(Calendar.DAY_OF_MONTH, i);
                    Integer targetDate = wdCalendar.getTime().getDate();
                    SalaryTable.SalaryTableCell cell = workingDays.stream()
                            .filter(wd -> targetDate.equals(wd.getDate().getDate()))
                            .map(WorkingDay::toPoint)
                            .findFirst().orElse(new SalaryTable.SalaryTableCell(wdCalendar.getTime(), employee));
                    row.add(cell);
                }
            } else {
                for (int i = 1; i <= monthBoundaries.getValue1().getDate(); i++) {
                    wdCalendar.set(Calendar.DAY_OF_MONTH, i);
                    Date targetDate = wdCalendar.getTime();
                    row.add(new SalaryTable.SalaryTableCell(targetDate, employee));
                }
            }
            salaryTable.addRow(row);
        }

        return salaryTable;
    }

    public List<SalaryTable.SalaryTableCell> getRowByDate(Employee employee, Date date) {
        Pair<Date,Date> monthBoundaries = Utils.getMonthBoundaries(date);

        List<WorkingDay> workingDays = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("employee"), employee));
            predicates.add(cb.between(root.get("date"), monthBoundaries.getValue0(), monthBoundaries.getValue1()));
            return cb.and(predicates.toArray(Predicate[]::new));
        });

        Calendar wdCalendar = GregorianCalendar.getInstance();
        List<SalaryTable.SalaryTableCell> row = new ArrayList<>();
        wdCalendar.setTime(monthBoundaries.getValue0());
        for (int i = 1; i <= monthBoundaries.getValue1().getDate(); i++) {
            wdCalendar.set(Calendar.DAY_OF_MONTH, i);
            Integer targetDate = wdCalendar.getTime().getDate();
            SalaryTable.SalaryTableCell cell = workingDays.stream()
                    .filter(wd -> targetDate.equals(wd.getDate().getDate()))
                    .map(WorkingDay::toPoint)
                    .findFirst().orElse(new SalaryTable.SalaryTableCell(wdCalendar.getTime(), employee));
            row.add(cell);
        }


        return row;
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

    public Map<Date, List<WorkingDay>> getWorkingDaysByOffsiteEmployees(Date startDate, Date endDate){
        List<Employee> offsiteEmployee = employeeDispatcher.getInstallersList();
        List<WorkingDay> workingDays = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("employee").in(offsiteEmployee));
            predicates.add(cb.between(root.get("date"), startDate, endDate));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, Sort.by(Sort.Direction.ASC, "employee"));
        return workingDays.stream().collect(Collectors.groupingBy(WorkingDay::getDate));
    }

    public Integer getSalarySumByDateRange(Employee employee, DateRange dateRange) {
        Float sum = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("employee"), employee));
            predicates.add(cb.between(root.get("date"), dateRange.getStart(), dateRange.getEnd()));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream()
                .map(WorkingDay::toPoint)
                .map(SalaryTable.SalaryTableCell::getSumWithoutNDFL)
                .reduce(0f, Float::sum);
        return Math.round(sum);
    }
}
