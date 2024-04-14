package com.microel.trackerbackend.storage.dispatchers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.modules.transport.DateRange;
import com.microel.trackerbackend.modules.transport.charts.BarChartData;
import com.microel.trackerbackend.modules.transport.charts.ChartDataset;
import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.salary.WorkCalculation;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.repositories.WorkLogRepository;
import lombok.Data;
import org.javatuples.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class StatisticsDispatcher {

    private final WorkLogRepository workLogRepository;

    public StatisticsDispatcher(WorkLogRepository workLogRepository) {
        this.workLogRepository = workLogRepository;
    }

    public EmployeeWorkStatisticsTable getEmployeeWorkStatistics(EmployeeWorkStatisticsForm form) {
        if (form.isInvalid())
            throw new ResponseException("Не корректные входные данные");

        List<WorkLog> closedWorkLogs = workLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.join("employees", JoinType.LEFT).get("login").in(form.getEmployees()));
            predicates.add(cb.between(root.get("created"), form.getPeriod().start(), form.getPeriod().end()));
            predicates.add(cb.isNotNull(root.get("closed")));
            predicates.add(cb.isTrue(root.get("calculated")));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream().distinct().toList();

        Map<Employee, List<WorkLog>> employeeWorkLogs = closedWorkLogs
                .stream()
                .collect(
                        Collectors.flatMapping(
                                workLog -> workLog.getEmployees().stream().map(employee -> Pair.with(employee, workLog)),
                                Collectors.groupingBy(Pair::getValue0, Collectors.mapping(Pair::getValue1, Collectors.toList()))
                        )
                );

        Map<Employee, List<WorkLog>> filteredEmployeeWorkLogs = new HashMap<>();
        for (Employee employee : employeeWorkLogs.keySet()) {
            if (form.employees.contains(employee.getLogin())) {
                filteredEmployeeWorkLogs.put(employee, employeeWorkLogs.get(employee));
            }
        }

        return EmployeeWorkStatisticsTable.of(filteredEmployeeWorkLogs);
    }

    @Data
    public static class EmployeeWorkStatisticsForm {
        private DateRange period;
        private List<String> employees;

        public boolean isInvalid() {
            return !period.validate() || employees == null || employees.isEmpty();
        }
    }

    @Data
    public static class EmployeeWorkStatisticsTable {

        @JsonIgnore
        private List<EmployeeRow> rowsList;

        public static EmployeeWorkStatisticsTable of(Map<Employee, List<WorkLog>> employeeWorkLogs) {
            EmployeeWorkStatisticsTable table = new EmployeeWorkStatisticsTable();
            table.rowsList = employeeWorkLogs.entrySet().stream().map(entry -> EmployeeRow.of(entry.getKey(), entry.getValue())).collect(Collectors.toList());

            return table;
        }

        public List<FlatEmployeeRow> getRows() {
            List<FlatEmployeeRow> flatRows = new ArrayList<>();
            for (EmployeeRow row : rowsList) {
                boolean firstEmployeeRow = true;
                int employeeRowSpan = row.rows.stream().map(cls -> cls.rows.size()).reduce(0, Integer::sum) + 3;
                for (EmployeeRow.TaskClass taskClass : row.rows) {
                    boolean firstTaskClass = true;
                    for (EmployeeRow.TaskClass.TaskType taskType : taskClass.rows) {
                        flatRows.add(FlatEmployeeRow.of(firstEmployeeRow ? row.employee : null, employeeRowSpan, firstTaskClass ? taskClass.name : null, taskClass.rows.size(), taskType));
                        firstEmployeeRow = false;
                        firstTaskClass = false;
                    }
                }
                if (row.rows.isEmpty()) continue;
                flatRows.add(FlatEmployeeRow.of(employeeRowSpan, row.zeroTask));
                flatRows.add(FlatEmployeeRow.of(employeeRowSpan, row.paidTask));
                flatRows.add(FlatEmployeeRow.of(employeeRowSpan, row.total));
            }
            return flatRows;
        }

        public BarChartData getTaskCountChart() {
            List<String> employeeLabels = rowsList.stream().map(EmployeeRow::getEmployee).map(Employee::getFullName).toList();

            Map<String, ChartDataset> datasets = new HashMap<>();

            for (EmployeeRow row : rowsList) {
                row.getRows().stream().map(taskClass -> {
                    ChartDataset chartDataset = ChartDataset.of(taskClass.name);
                    chartDataset.setStack("mainStack");
                    return chartDataset;
                }).forEach(cd -> {
                    datasets.putIfAbsent(cd.getLabel(), cd);
                });
            }

            for (EmployeeRow row : rowsList) {
                for (ChartDataset dataset : datasets.values()) {
                    EmployeeRow.TaskClass taskClass = row.getRows().stream().filter(tc -> tc.name.equals(dataset.getLabel())).findFirst().orElse(null);
                    if (taskClass == null) {
                        dataset.getData().add(null);
                        continue;
                    }
                    datasets.get(taskClass.name).getData().add(taskClass.rows.stream().mapToDouble(tt -> tt.count).sum());
                }
                datasets.computeIfAbsent("Ср. за смену", (key) -> {
                    ChartDataset chartDataset = ChartDataset.of("Ср. за смену");
                    chartDataset.setStack("secondStack");
                    return chartDataset;
                });
                datasets.get("Ср. за смену").getData().add(row.getTotal().getTaskQuantityPerShift().doubleValue());
            }

            return BarChartData.of(employeeLabels, datasets.values().stream().toList());
        }

        public BarChartData getTimingsChart() {
            List<String> employeeLabels = rowsList.stream().map(EmployeeRow::getEmployee).map(Employee::getFullName).toList();
            Map<String, ChartDataset> datasets = new HashMap<>();


            for (EmployeeRow row : rowsList) {
                datasets.putIfAbsent("Отд./Прин.", ChartDataset.of("Отд./Прин."));
                datasets.putIfAbsent("Отд./Закр.", ChartDataset.of("Отд./Закр."));
                datasets.putIfAbsent("Прин./Закр.", ChartDataset.of("Прин./Закр."));
                datasets.putIfAbsent("Δ Между задачами", ChartDataset.of("Δ Между задачами"));
                datasets.get("Отд./Прин.").getData().add(row.getTotal().getTimings().getGivenAndReceived().doubleValue());
                datasets.get("Отд./Закр.").getData().add(row.getTotal().getTimings().getGivenAndClosed().doubleValue());
                datasets.get("Прин./Закр.").getData().add(row.getTotal().getTimings().getReceivedAndClosed().doubleValue());
                datasets.get("Δ Между задачами").getData().add(row.getTotal().getTimings().getDelayBetween().doubleValue());
            }

            return BarChartData.of(employeeLabels, datasets.values().stream().toList());
        }

        public BarChartData getMoneyChart() {
            List<String> employeeLabels = rowsList.stream().map(EmployeeRow::getEmployee).map(Employee::getFullName).toList();
            Map<String, ChartDataset> datasets = new HashMap<>();

            for (EmployeeRow row : rowsList) {
                datasets.putIfAbsent("За смену", ChartDataset.of("За смену"));
                datasets.putIfAbsent("За час", ChartDataset.of("За час"));
                datasets.putIfAbsent("За задачу", ChartDataset.of("За задачу"));
                datasets.get("За смену").getData().add(row.getTotal().getMoney().getQuantityPerShift().doubleValue());
                datasets.get("За час").getData().add(row.getTotal().getMoney().getQuantityPerHour().doubleValue());
                datasets.get("За задачу").getData().add(row.getTotal().getMoney().getQuantityPerTask().doubleValue());
            }

            return BarChartData.of(employeeLabels, datasets.values().stream().toList());
        }

        @Data
        public static class FlatEmployeeRow {
            @Nullable
            private Employee employee;
            private Integer employeeRowSpanCount;
            @Nullable
            private String className;
            private Integer taskClassRowSpanCount;
            @Nullable
            private EmployeeRow.TaskClass.TaskType taskType;
            @Nullable
            private EmployeeRow.ZeroTask zeroTask;
            @Nullable
            private EmployeeRow.PaidTask paidTask;
            @Nullable
            private EmployeeRow.Total total;

            public static FlatEmployeeRow of(Employee employee, Integer employeeRowSpan, String className, Integer classRowSpan, EmployeeRow.TaskClass.TaskType taskType) {
                FlatEmployeeRow row = new FlatEmployeeRow();
                row.employee = employee;
                row.employeeRowSpanCount = employeeRowSpan;
                row.className = className;
                row.taskClassRowSpanCount = classRowSpan;
                row.taskType = taskType;
                return row;
            }

            public static FlatEmployeeRow of(Integer employeeRowSpan, EmployeeRow.ZeroTask zeroTask) {
                FlatEmployeeRow row = new FlatEmployeeRow();
                row.employee = null;
                row.employeeRowSpanCount = employeeRowSpan;
                row.className = "Нулевые задачи";
                row.taskClassRowSpanCount = zeroTask.getTaskCount();
                row.taskType = null;
                row.zeroTask = zeroTask;
                return row;
            }

            public static FlatEmployeeRow of(Integer employeeRowSpan, EmployeeRow.PaidTask paidTask) {
                FlatEmployeeRow row = new FlatEmployeeRow();
                row.employee = null;
                row.employeeRowSpanCount = employeeRowSpan;
                row.className = "Платные задачи";
                row.taskClassRowSpanCount = paidTask.getTaskCount();
                row.taskType = null;
                row.paidTask = paidTask;
                return row;
            }

            public static FlatEmployeeRow of(Integer employeeRowSpan, EmployeeRow.Total total) {
                FlatEmployeeRow row = new FlatEmployeeRow();
                row.employee = null;
                row.employeeRowSpanCount = employeeRowSpan;
                row.className = "Итог";
                row.taskClassRowSpanCount = total.getTaskCount();
                row.taskType = null;
                row.total = total;
                return row;
            }
        }

        @Data
        public static class EmployeeRow {
            private Employee employee;
            private List<TaskClass> rows;
            private ZeroTask zeroTask;
            private PaidTask paidTask;
            private Total total;

            public static EmployeeRow of(Employee employee, List<WorkLog> workLogs) {
                List<WorkLog> normalWorkLogs = workLogs.stream()
                        .filter(workLog -> {
                            return workLog.getWorkCalculations().stream().noneMatch(WorkCalculation::getIsPaidWork) &&
                                    workLog.getWorkCalculations().stream().allMatch(WorkCalculation::isNotZero);
                        }).toList();
                List<WorkLog> zeroWorkLogs = workLogs.stream()
                        .filter(workLog -> workLog.getWorkCalculations().stream().noneMatch(WorkCalculation::getIsPaidWork) &&
                                workLog.getWorkCalculations().stream().noneMatch(WorkCalculation::isNotZero)).toList();
                List<WorkLog> paidWorkLogs = workLogs.stream()
                        .filter(workLog -> workLog.getWorkCalculations().stream().anyMatch(WorkCalculation::getIsPaidWork)).toList();
                EmployeeRow row = new EmployeeRow();
                row.employee = employee;
                Map<Wireframe, List<WorkLog>> wireframeWorkLogs = normalWorkLogs.stream().collect(Collectors.groupingBy(workLog -> workLog.getTask().getModelWireframe()));
                row.rows = wireframeWorkLogs.entrySet().stream().map(entry -> TaskClass.of(entry.getKey().getName(), entry.getValue(), employee)).collect(Collectors.toList());
                row.zeroTask = ZeroTask.of(zeroWorkLogs, employee);
                row.paidTask = PaidTask.of(paidWorkLogs, employee);
                row.total = Total.of(normalWorkLogs, employee);
                return row;
            }

            @Data
            public static class ZeroTask {
                private Integer taskCount;
                private Long taskQuantityPerShift;
                private TaskClass.TaskType.Timings timings;
                private List<WorkLog> workLogs;

                public static ZeroTask of(List<WorkLog> workLogs, Employee employee) {
                    ZeroTask zeroTask = new ZeroTask();
                    zeroTask.taskCount = workLogs.size();
                    zeroTask.taskQuantityPerShift = Math.round(workLogs
                            .stream()
                            .collect(
                                    Collectors.groupingBy(
                                            workLog -> {
                                                return workLog.getClosed().toLocalDateTime().toLocalDate();
                                            }
                                    )
                            ).values().stream().mapToInt(List::size).average().orElse(0f));
                    zeroTask.timings = TaskClass.TaskType.Timings.of(workLogs);
                    zeroTask.workLogs = workLogs;
                    return zeroTask;
                }
            }

            @Data
            public static class PaidTask {
                private Integer taskCount;
                private Long taskQuantityPerShift;
                private TaskClass.TaskType.Timings timings;
                private TaskClass.TaskType.Money money;

                public static PaidTask of(List<WorkLog> workLogs, Employee employee) {
                    PaidTask paidTask = new PaidTask();
                    paidTask.taskCount = workLogs.size();
                    paidTask.taskQuantityPerShift = Math.round(workLogs
                            .stream()
                            .collect(
                                    Collectors.groupingBy(
                                            workLog -> {
                                                return workLog.getClosed().toLocalDateTime().toLocalDate();
                                            }
                                    )
                            ).values().stream().mapToInt(List::size).average().orElse(0f));
                    paidTask.timings = TaskClass.TaskType.Timings.of(workLogs);
                    paidTask.money = TaskClass.TaskType.Money.of(workLogs, employee);
                    return paidTask;
                }
            }

            @Data
            public static class Total {
                private Integer taskCount;
                private Long taskQuantityPerShift;
                private TaskClass.TaskType.Timings timings;
                private TaskClass.TaskType.Money money;

                public static Total of(List<WorkLog> workLogs, Employee employee) {
                    Total total = new Total();
                    total.taskCount = workLogs.size();
                    total.taskQuantityPerShift = Math.round(workLogs
                            .stream()
                            .collect(
                                    Collectors.groupingBy(
                                            workLog -> {
                                                return workLog.getClosed().toLocalDateTime().toLocalDate();
                                            }
                                    )
                            ).values().stream().mapToInt(List::size).average().orElse(0f));
                    total.timings = TaskClass.TaskType.Timings.of(workLogs);
                    total.money = TaskClass.TaskType.Money.of(workLogs, employee);
                    return total;
                }
            }

            @Data
            public static class TaskClass {
                private String name;
                private List<TaskType> rows;

                public static TaskClass of(String name, List<WorkLog> workLogs, Employee employee) {
                    TaskClass taskClass = new TaskClass();
                    taskClass.name = name;
                    Map<TaskStage, List<WorkLog>> taskStageWorkLogs = workLogs.stream().collect(Collectors.groupingBy(workLog -> workLog.getTask().getCurrentStage()));
                    taskClass.rows = taskStageWorkLogs.entrySet().stream().map(entry -> TaskType.of(entry.getKey().getLabel(), entry.getValue(), employee)).collect(Collectors.toList());
                    return taskClass;
                }

                @Data
                public static class TaskType {
                    private String name;
                    private Integer count;
                    private Long quantityPerShift;
                    private Timings timings;
                    private Money money;

                    public static TaskType of(String name, List<WorkLog> workLogs, Employee employee) {
                        TaskType taskType = new TaskType();
                        taskType.name = name;
                        taskType.count = workLogs.size();

                        taskType.quantityPerShift = Math.round(workLogs
                                .stream()
                                .collect(
                                        Collectors.groupingBy(
                                                workLog -> {
                                                    return workLog.getClosed().toLocalDateTime().toLocalDate();
                                                }
                                        )
                                ).values().stream().mapToInt(List::size).average().orElse(0f));

                        taskType.timings = Timings.of(workLogs);
                        taskType.money = Money.of(workLogs, employee);
                        return taskType;
                    }

                    @Data
                    public static class Timings {
                        private Long delayBetween;
                        private Long givenAndReceived;
                        private Long givenAndClosed;
                        private Long receivedAndClosed;
                        private Long timeSpentOnTasks;

                        public static Timings of(List<WorkLog> workLogs) {
                            Timings timings = new Timings();

                            Map<LocalDate, List<WorkLog>> workLogsPerDay = workLogs.stream()
                                    .collect(
                                            Collectors.groupingBy(
                                                    workLog -> {
                                                        return workLog.getClosed().toLocalDateTime().toLocalDate();
                                                    }
                                            )
                                    );

                            List<Pair<Timestamp, Timestamp>> closedAndGivenPairs = new ArrayList<>();

                            for (List<WorkLog> workLogInDay : workLogsPerDay.values())
                                for (int i = 0; i < workLogInDay.size() - 1; i++) {
                                    WorkLog workLog = workLogInDay.get(i);
                                    WorkLog nextWorkLog = workLogInDay.get(i + 1);

                                    if (workLog.getClosed() != null && nextWorkLog.getCreated() != null) {
                                        closedAndGivenPairs.add(new Pair<>(workLog.getClosed(), nextWorkLog.getFirstEmployeeAccept()));
                                    }
                                }


                            timings.delayBetween = Math.round(closedAndGivenPairs.stream()
                                    .filter(pair -> pair.getValue0()!= null && pair.getValue1()!= null)
                                    .mapToLong(pair -> Duration.between(pair.getValue0().toInstant(), pair.getValue1().toInstant()).toMillis())
                                    .average()
                                    .orElse(0d));

                            timings.givenAndReceived = Math.round(workLogs.stream()
                                    .map(WorkLog::getGivenAndReceivedDuration)
                                    .mapToLong(Duration::toMillis)
                                    .average()
                                    .orElse(0d));

                            timings.givenAndClosed = Math.round(workLogs.stream()
                                    .map(WorkLog::getGivenAndClosedDuration)
                                    .mapToLong(Duration::toMillis)
                                    .average()
                                    .orElse(0d));

                            timings.receivedAndClosed = Math.round(workLogs.stream()
                                    .map(WorkLog::getReceivedAndClosedDuration)
                                    .mapToLong(Duration::toMillis)
                                    .average()
                                    .orElse(0d));

                            timings.timeSpentOnTasks = workLogs.stream()
                                    .map(WorkLog::getReceivedAndClosedDuration)
                                    .mapToLong(Duration::toMillis)
                                    .sum();

                            return timings;
                        }
                    }

                    @Data
                    public static class Money {
                        private Long quantityPerShift;
                        private Long quantityPerHour;
                        private Long quantityPerHourActual;
                        private Long quantityPerTask;
                        private Long quantityAll;

                        public static Money of(List<WorkLog> workLogs, Employee employee) {
                            Money money = new Money();

                            Map<LocalDate, List<WorkLog>> workLogsPerDay = workLogs.stream()
                                    .collect(
                                            Collectors.groupingBy(
                                                    workLog -> {
                                                        return workLog.getClosed().toLocalDateTime().toLocalDate();
                                                    }
                                            )
                                    );

                            money.quantityPerShift =
                                    Math.round(workLogsPerDay.values().stream()
                                            .mapToDouble(workLogsS ->
                                                    workLogsS.stream()
                                                            .map(workLog -> workLog.getSalaryByEmployee(employee))
                                                            .filter(Objects::nonNull)
                                                            .mapToDouble(Float::doubleValue)
                                                            .sum()
                                            ).average().orElse(0d));


                            money.quantityPerHour = Math.round(money.quantityPerShift / 11d);

                            money.quantityPerTask = Math.round(
                                    workLogs.stream()
                                            .mapToDouble(
                                                    workLog -> workLog.getSalaryByEmployee(employee)
                                            ).average().orElse(0d)
                            );

                            double quantityAll = workLogs.stream()
                                    .mapToDouble(
                                            workLog -> workLog.getSalaryByEmployee(employee)
                                    ).sum();

                            money.quantityAll = Math.round(quantityAll);

                            double hoursAll = workLogs.stream()
                                    .map(WorkLog::getReceivedAndClosedDuration)
                                    .mapToLong(Duration::toMillis)
                                    .sum() / 3600000d;
                            money.quantityPerHourActual = hoursAll == 0L ? 0L : Math.round(quantityAll / hoursAll);

                            return money;
                        }
                    }
                }
            }
        }
    }
}
