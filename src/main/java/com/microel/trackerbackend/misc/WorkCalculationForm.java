package com.microel.trackerbackend.misc;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class WorkCalculationForm {
    private Long workLogId;
    @Nullable
    private String emptyDescription;
    @Nullable
    private String editingDescription;
    private List<ActionCalculationItem> actions;
    private List<SpreadingItem> spreading;
    private Boolean isPaidWork;
    @Nullable
    private Float amountOfMoneyTaken;
    @Nullable
    private String comment;
    private Boolean isLegalEntity;

    @Getter
    @Setter
    @Builder
    public static class ActionCalculationItem {
        @Nullable
        private Long workId;
        private Long actionId;
        private Integer count;
        private UUID uuid;
    }

    @Getter
    @Setter
    @Builder
    public static class SpreadingItem {
        private String login;
        private Float ratio;
        @Nullable
        private List<FactorAction> factorsActions;
    }
}
