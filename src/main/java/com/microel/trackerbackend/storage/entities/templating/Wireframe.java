package com.microel.trackerbackend.storage.entities.templating;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.StepItem;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeDef(name = "json", typeClass = JsonType.class)
@Table(name = "wireframes")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Wireframe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wireframeId;
    private WireframeType wireframeType;
    @Column(length = 64)
    private String name;
    @Column(length = 512)
    private String description;
    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private List<StepItem> steps;
    @OneToMany(mappedBy = "wireframe", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @BatchSize(size = 25)
    @OrderBy(value = "orderIndex")
    private List<TaskStage> stages;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private List<DefaultObserver> defaultObservers;
    private Timestamp created;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_employee_id")
    private Employee creator;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    private String listViewType;
    private String detailedViewType;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
    @JoinColumn(name = "f_wireframe_id")
    @BatchSize(size = 25)
    private List<DocumentTemplate> documentTemplates;

    public static Wireframe toDropdownList(Wireframe source) {
        Wireframe newWireframe = new Wireframe();
        newWireframe.setWireframeId(source.getWireframeId());
        newWireframe.setName(source.getName());
        newWireframe.setDeleted(source.getDeleted());
        return newWireframe;
    }

    public void appendStage(TaskStage stage) {
        if (stage.getWireframe() == null) stage.setWireframe(this);
        if (this.stages == null) this.stages = new ArrayList<>();
        this.stages.add(stage);
    }

    public void setStages(List<TaskStage> stages) {
        if (stages == null) {
            this.stages = new ArrayList<>();
            return;
        }
        for (TaskStage stage : stages) {
            if (stage.getWireframe() == null) stage.setWireframe(this);
        }
        this.stages = stages;
    }

    public void setDefaultObservers(List<DefaultObserver> newObservers) {
        if (newObservers == null) return;
        if (this.defaultObservers == null)
            this.defaultObservers = new ArrayList<>();
        this.defaultObservers.removeIf(defObs -> newObservers.stream().noneMatch(nobs -> Objects.equals(defObs, nobs)));
        newObservers.removeIf(defObs -> this.defaultObservers.contains(defObs));
        this.defaultObservers.addAll(newObservers);
    }

    /**
     * Создает список всех шаблонов полей из шаблона
     *
     * @return Список полей
     */
    public List<FieldItem> getAllFields() {
        if (steps == null) return new ArrayList<>();
        return steps.stream().map(StepItem::getFields).reduce(new ArrayList<>(), (a, b) -> {
            a.addAll(b);
            return a;
        }).stream().sorted(Comparator.comparing(FieldItem::getOrderPosition)).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    public TaskStage getFirstStage() {
        return getStages().stream().filter(stage -> stage.getOrderIndex() == 0).findFirst().orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wireframe wireframe)) return false;
        return Objects.equals(getWireframeId(), wireframe.getWireframeId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWireframeId());
    }

    public void clearRemovedStages(List<TaskStage.Form> existedStages) {
        Iterator<TaskStage> iterator = this.stages.iterator();
        while (iterator.hasNext()) {
            TaskStage stage  = iterator.next();
            if (existedStages.stream().noneMatch(s -> Objects.equals(s.getStageId(), stage.getStageId())))  {
                stage.setWireframe(null);
                iterator.remove();
            }
        }
    }
//    public void clearRemovedStages(List<TaskStage.Form> existedStages) {
//        for (TaskStage stage : getStages()) {
//            if (existedStages.stream().noneMatch(s -> Objects.equals(s.getStageId(), stage.getStageId()))) {
//                stage.setWireframe(null);
//                getStages().remove(stage);
//            }
//        }
//    }

    @Data
    public static class Form {
        private String name;
        private String description;
        private String listViewType;
        private String detailedViewType;
        private List<DefaultObserver> defaultObservers;
        private List<TaskStage.Form> stages;
        private List<StepItem> steps;
        private WireframeType wireframeType;
        private List<DocumentTemplate.Form> documentTemplates;
    }

}
