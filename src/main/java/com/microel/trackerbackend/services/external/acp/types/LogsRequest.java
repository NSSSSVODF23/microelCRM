package com.microel.trackerbackend.services.external.acp.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LogsRequest {
    private List<Log> logs = new ArrayList<>();
    private Boolean isLast = true;
    private Integer currentPage = 0;

    @Getter
    @Setter
    public static class Log {
        private Timestamp startDatetime;
        private Timestamp endDatetime;
        @JsonIgnore
        private List<DhcpSessions> events;
        private Type type;
        private String description;
        private Integer numberRepetitions;
        private String macAddresses;

        public enum Type {
            SIMPLE_ONLINE("SIMPLE_ONLINE"),
            SIMPLE_OFFLINE("SIMPLE_OFFLINE"),
            REPEATED("REPEATED"),
            USER_AUTH("USER_AUTH"),
            USER_AUTH_FAIL("USER_AUTH_FAIL"),
            EMPTY("EMPTY");

            private final String type;

            Type(String type) {
                this.type = type;
            }

            public String getType() {
                return type;
            }
        }
    }
}
