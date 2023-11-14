package com.microel.trackerbackend.misc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
public class DataPair {
    private String label;
    private Object value;
    @Nullable
    private String color;
    @Nullable
    private String lcolor;
    @Nullable
    private String vcolor;

    public static DataPair of(String label, Object value) {
        DataPair pair = new DataPair();
        pair.setLabel(label);
        pair.setValue(value);
        return pair;
    }

    public static DataPair of(String label, Object value, String color) {
        DataPair pair = new DataPair();
        pair.setLabel(label);
        pair.setValue(value);
        pair.setColor(color);
        return pair;
    }

    public static DataPair of(String label, Object value, String lcolor, String vcolor) {
        DataPair pair = new DataPair();
        pair.setLabel(label);
        pair.setValue(value);
        pair.setLcolor(lcolor);
        pair.setVcolor(vcolor);
        return pair;
    }

    public static DataPair of(String label, Object value, String color, String lcolor, String vcolor) {
        DataPair pair = new DataPair();
        pair.setLabel(label);
        pair.setValue(value);
        pair.setColor(color);
        pair.setLcolor(lcolor);
        pair.setVcolor(vcolor);
        return pair;
    }
}
