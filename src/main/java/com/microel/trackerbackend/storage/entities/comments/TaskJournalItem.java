package com.microel.trackerbackend.storage.entities.comments;

import com.microel.trackerbackend.storage.entities.team.Employee;

import java.sql.Timestamp;

public interface TaskJournalItem {
    String getMessage();
    Timestamp getCreated();
    Employee getCreator();
}
