package com.microel.trackerbackend.misc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class WireframeTaskCounter {
    private Long id;
    private Long num;
    @Nullable
    private Set<StageTaskCounter> stages;

    public static WireframeTaskCounter of(Long wireframeId, Long num, Map<String, Long> stages) {
        WireframeTaskCounter counter = new WireframeTaskCounter();
        counter.setId(wireframeId);
        counter.setNum(num);
        counter.setStages(stages.entrySet().stream().map(entry->StageTaskCounter.of(entry.getKey(),entry.getValue())).collect(Collectors.toSet()));
        return counter;
    }

    public static WireframeTaskCounter of(Long wireframeId, Long num) {
        WireframeTaskCounter counter = new WireframeTaskCounter();
        counter.setId(wireframeId);
        counter.setNum(num);
        return counter;
    }

    @Getter
    @Setter
    public static class StageTaskCounter{
        private String id;
        private Long num;

        public static StageTaskCounter of(String id, Long num) {
            StageTaskCounter counter = new StageTaskCounter();
            counter.setId(id);
            counter.setNum(num);
            return counter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StageTaskCounter that)) return false;
            return Objects.equals(getId(), that.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WireframeTaskCounter that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
