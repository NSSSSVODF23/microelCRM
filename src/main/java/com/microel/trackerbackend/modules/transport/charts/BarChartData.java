package com.microel.trackerbackend.modules.transport.charts;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BarChartData {
    private List<String> labels = new ArrayList<>();
    private List<ChartDataset> datasets = new ArrayList<>();

    public static BarChartData of(List<String> labels, List<ChartDataset> datasets) {
        BarChartData barChartData = new BarChartData();
        barChartData.setLabels(labels);
        barChartData.setDatasets(datasets);
        datasets.forEach(dataset -> dataset.setType("bar"));
        return barChartData;
    }
}
