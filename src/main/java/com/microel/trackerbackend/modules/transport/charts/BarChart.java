package com.microel.trackerbackend.modules.transport.charts;

import lombok.Data;

@Data
public class BarChart {
    private String type = "bar";
    private BarChartData data;

    public static BarChart of(BarChartData data) {
        BarChart chart = new BarChart();
        chart.setData(data);
        return chart;
    }
}
