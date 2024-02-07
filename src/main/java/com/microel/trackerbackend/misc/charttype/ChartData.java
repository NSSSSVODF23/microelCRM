package com.microel.trackerbackend.misc.charttype;

import lombok.*;
import org.springframework.lang.Nullable;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class ChartData {
    @Nullable
    private List<String> labels;
    private List<ChartDataSet> datasets;
}
