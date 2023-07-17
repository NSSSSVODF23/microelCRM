package com.microel.trackerbackend.misc;

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
    private List<ActionCalculationItem> actions;
    private List<SpreadingItem> spreading;

    @Getter
    @Setter
    public static class ActionCalculationItem {
        @Nullable
        private Long workId;
        private Long actionId;
        private Integer count;
        private UUID uuid;
    }

    @Getter
    @Setter
    public static class SpreadingItem {
        private String login;
        private Float ratio;
        @Nullable
        private List<FactorAction> factorsActions;
    }
}
