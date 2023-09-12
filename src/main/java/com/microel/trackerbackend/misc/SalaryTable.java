package com.microel.trackerbackend.misc;

import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;

import java.time.YearMonth;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryTable {
    private List<String> headers = new ArrayList<>();
    private List<Employee> employees = new ArrayList<>();
    private List<List<SalaryTableCell>> payload = new ArrayList<>();

    public void setHeaders(Date start){
        headers.add("Сотрудник");
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(start);
        gc.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i = 1; i <= gc.getActualMaximum(Calendar.DAY_OF_MONTH); i++){
            headers.add(String.valueOf(i));
        }
        headers.add("Сумма");
    }

    public List<SalaryTableTotalCell> getTotalSum() {
        List<SalaryTableTotalCell> salaryTableTotalCells = new ArrayList<>();
        int i = 0;
        for(Employee employee : employees){
            SalaryTableTotalCell salaryTableTotalCell = new SalaryTableTotalCell();
            List<SalaryTableCell> row = payload.get(i);
            salaryTableTotalCell.setEmployee(employee);
            salaryTableTotalCell.setSumWithNDFL(Math.round(row.stream().map(SalaryTableCell::getSumWithNDFL).reduce(0f, Float::sum)));
            salaryTableTotalCell.setSumWithoutNDFL(Math.round(row.stream().map(SalaryTableCell::getSumWithoutNDFL).reduce(0f, Float::sum)));
            salaryTableTotalCells.add(salaryTableTotalCell);
            i++;
        }
        return salaryTableTotalCells;
    }

    public SalaryTableTotalCell getTotalSumAllEmployees(){
        SalaryTableTotalCell salaryTableTotalCell = new SalaryTableTotalCell();
        for (int i = 0; i < employees.size(); i++) {
            List<SalaryTableCell> row = payload.get(i);
            salaryTableTotalCell.addSumWithNDFL(Math.round(row.stream().map(SalaryTableCell::getSumWithNDFL).reduce(0f, Float::sum)));
            salaryTableTotalCell.addSumWithoutNDFL(Math.round(row.stream().map(SalaryTableCell::getSumWithoutNDFL).reduce(0f, Float::sum)));
        }
        return salaryTableTotalCell;
    }

    public void addRow(List<SalaryTableCell> row){
        payload.add(row);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalaryTableCell {
        private Employee employee;
        private Float sumWithNDFL = 0f;
        private Float sumWithoutNDFL = 0f;
        private Date date;

        public SalaryTableCell(Date date, Employee employee){
            this.date = date;
            this.employee = employee;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalaryTableTotalCell{
        private Employee employee;
        private Integer sumWithNDFL = 0;
        private Integer sumWithoutNDFL = 0;

        public void addSumWithNDFL(Integer sumWithNDFL){
            this.sumWithNDFL += sumWithNDFL;
        }

        public void addSumWithoutNDFL(Integer sumWithoutNDFL){
            this.sumWithoutNDFL += sumWithoutNDFL;
        }
    }
}
