package com.microel.trackerbackend.misc;

import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Отправляется в ui для заполнения форм подсчета зарплаты, в случаях когда открывается уже посчитанный WorkLog (редактируется)
 */
@Getter
@Setter
@Builder
public class ResponseWorkEstimationForm {

    private List<WorkActionFormItem> actions;
    private List<FactorWorkActionFormItem> factorsActions;
    private Map<String, EmployeeRatioValue> employeesRatio;
    private Boolean isPaidWork;
    private Float amountOfMoneyTaken;
    private String comment;
    private Boolean isLegalEntity;

    @Getter
    @Setter
    @Builder
    public static class WorkActionFormItem {
        private String workName;
        private Long workId;
        private String actionName;
        private Integer count;
        private PaidAction.Unit unit;
        private Float price;
        private Float cost;
        private Long actionId;
        private UUID uuid;
    }

    @Getter
    @Setter
    @Builder
    public static class FactorWorkActionFormItem {
        private Float factor;
        private String login;
        private String name;
        private List<UUID> actionUuids;
        private UUID uuid;
    }

    @Getter
    @Setter
    @Builder
    public static class EmployeeRatioValue{
        private Float ratio;
        private Float sum;
    }

}
