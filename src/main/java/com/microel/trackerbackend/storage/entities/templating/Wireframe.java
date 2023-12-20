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
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
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
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "f_wireframe_id")
    @BatchSize(size = 25)
    private Set<TaskStage> stages;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
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

    /**
     * Создает список всех шаблонов полей из шаблона
     * @return Список полей
     */
    public List<FieldItem> getAllFields(){
        if(steps == null) return new ArrayList<>();
        return steps.stream().map(StepItem::getFields).reduce(new ArrayList<>(), (a, b) -> {
            a.addAll(b);
            return a;
        }).stream().sorted(Comparator.comparing(FieldItem::getOrderPosition)).collect(Collectors.toList());
    }

    public static Wireframe toDropdownList(Wireframe source){
        Wireframe newWireframe = new Wireframe();
        newWireframe.setWireframeId(source.getWireframeId());
        newWireframe.setName(source.getName());
        newWireframe.setDeleted(source.getDeleted());
        return newWireframe;
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    public TaskStage getFirstStage(){
        return getStages().stream().filter(stage -> stage.getOrderIndex() == 0).findFirst().orElse(null);
    }

    @Data
    public static class Form{
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
