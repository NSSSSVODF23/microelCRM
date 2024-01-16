package com.microel.trackerbackend.misc;

public enum TimeFrame {
    NEXT_MONTH("NEXT_MONTH"),
    NEXT_WEEK("NEXT_WEEK"),
    TOMORROW("TOMORROW"),
    TODAY("TODAY"),
    YESTERDAY("YESTERDAY"),
    THIS_WEEK("THIS_WEEK"),
    LAST_WEEK("LAST_WEEK"),
    THIS_MONTH("THIS_MONTH"),
    LAST_MONTH("LAST_MONTH");
    private final String value;
    TimeFrame(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
