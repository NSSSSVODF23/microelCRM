package com.microel.trackerbackend.misc.charttype;

import lombok.*;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class ChartDataSet {
    @Nullable
    private String label;
    private List<Object> data;

    public void setDataOfDouble(List<Double> data) {
        this.data = Arrays.asList(data.toArray());
    }

    public void setDataOfFloat(List<Float> data){
        this.data = Arrays.asList(data.toArray());
    }

    public void setDataOfInt(List<Integer> data){
        this.data = Arrays.asList(data.toArray());
    }

    public void setDataOfLong(List<Long> data){
        this.data = Arrays.asList(data.toArray());
    }

    public void setDataOfPoints(List<ChartPoint> data){
        this.data = Arrays.asList(data.toArray());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartPoint{
        private Instant x;
        private Object y;
    }

}
