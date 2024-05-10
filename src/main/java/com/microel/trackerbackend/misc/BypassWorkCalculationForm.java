package com.microel.trackerbackend.misc;

import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class BypassWorkCalculationForm {

    private Task.CreationBody taskInfo;
    private InstallersReportForm reportInfo;
    private List<WorkCalculationForm.ActionCalculationItem> actions;
    private List<WorkCalculationForm.SpreadingItem> spreading;
    private Boolean isPaidWork;
    @Nullable
    private Float amountOfMoneyTaken;
    @Nullable
    private String comment;
    private Boolean isLegalEntity;

    @Getter
    @Setter
    public static class InstallersReportForm {
        private Set<Employee> installers;
        private String report;
        private Set<TaskTag> tags;
        private Timestamp date;

        public Timestamp getDate(){
            return Timestamp.from(Utils.trimDate(Date.from(date.toInstant())).toInstant());
        }
    }
}
