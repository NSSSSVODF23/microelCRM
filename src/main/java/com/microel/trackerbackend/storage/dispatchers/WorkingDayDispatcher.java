package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.SalaryRow;
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

    public void addCalculation(String login, Date date, WorkCalculation calculation){
        List<WorkingDay> all = workingDayRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.join("employee").get("login"), login));
            predicates.add(cb.equal(root.get("date"), date));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
        if(all.size() == 0){
            Employee employee = employeeDispatcher.getEmployee(login);
            WorkingDay workingDay = WorkingDay.builder()
                    .employee(employee)
                    .date(date)
                    .calculations(Stream.of(calculation).collect(Collectors.toList()))
                    .build();
            stompController.createWorkingDay(workingDayRepository.save(workingDay));
        }else{
            all.get(0).getCalculations().add(calculation);
            stompController.updateWorkingDay(workingDayRepository.save(all.get(0)));
        }
    }

    public List<SalaryRow> getTableByDate(@Nullable Date date, @Nullable Long position) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date == null ? new Date() : date);

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
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

        List<SalaryRow> salaryRows = new ArrayList<>();

        for(Employee employee : employeesByPosition) {
            if(rows.containsKey(employee)){
                salaryRows.add(SalaryRow.builder()
                        .employee(employee)
                        .salaryPoints(rows.get(employee).stream().map(WorkingDay::toSalaryPoint).collect(Collectors.toList()))
                        .build());
            }else{
                salaryRows.add(SalaryRow.builder()
                        .employee(employee)
                        .salaryPoints(new ArrayList<>())
                        .build());
            }
        }

        return salaryRows;
    }
}
