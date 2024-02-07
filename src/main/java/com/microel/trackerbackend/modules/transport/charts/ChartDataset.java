package com.microel.trackerbackend.modules.transport.charts;

import com.microel.trackerbackend.misc.ColorGenerator;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChartDataset {
    private String type;
    private String label = "";
    private List<Double> data = new ArrayList<>();
    @Nullable
    private String borderColor;
    @Nullable
    private String backgroundColor;
    @Nullable
    private String stack;

    public static ChartDataset of(String label, List<Double> data, String borderColor, String backgroundColor) {
        ChartDataset chartDataset = new ChartDataset();
        chartDataset.label = label;
        chartDataset.data = data;
        chartDataset.borderColor = borderColor;
        chartDataset.backgroundColor = backgroundColor;
        chartDataset.stack = label;
        return chartDataset;
    }

    public static ChartDataset of(String label, List<Double> data) {
        return of(label, data, ColorGenerator.fromStringToHsl(label, 70, 100, 50, 50), ColorGenerator.fromStringToHsl(label));
    }

    public static ChartDataset of(String label) {
        return of(label, new ArrayList<>());
    }

    public static ChartDataset of(String label, String borderColor, String backgroundColor) {
        return of(label, new ArrayList<>(), borderColor, backgroundColor);
    }
}
