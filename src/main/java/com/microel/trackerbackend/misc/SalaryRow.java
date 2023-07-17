package com.microel.trackerbackend.misc;

import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryRow {
    private Employee employee;
    private List<SalaryPoint> salaryPoints;

    public Integer getSum() {
        return salaryPoints.stream().map(SalaryPoint::getValue).reduce(0, Integer::sum);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalaryPoint {
        private Integer value;
        private Date date;
    }
}
