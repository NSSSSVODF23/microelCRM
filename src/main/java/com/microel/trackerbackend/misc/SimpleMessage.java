package com.microel.trackerbackend.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SimpleMessage {
    public enum Severity {
        INFO("INFO"),
        WARNING("WARNING"),
        ERROR("ERROR");
        private String value;
        Severity(String value) {
            this.value = value;
        }
    }
    private Severity severity;
    private String message;
}
