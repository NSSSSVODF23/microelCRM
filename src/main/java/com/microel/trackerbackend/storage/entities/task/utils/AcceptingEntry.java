package com.microel.trackerbackend.storage.entities.task.utils;

import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AcceptingEntry {
    private String login;
    private String telegramUserId;
    private Timestamp timestamp;

    public static AcceptingEntry of(Employee employee, Timestamp timestamp){
        return new AcceptingEntry(employee.getLogin(), employee.getTelegramUserId(), timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AcceptingEntry)) return false;
        AcceptingEntry that = (AcceptingEntry) o;
        return getLogin().equals(that.getLogin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLogin());
    }
}
