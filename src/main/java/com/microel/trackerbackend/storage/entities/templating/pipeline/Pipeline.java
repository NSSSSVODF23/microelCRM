package com.microel.trackerbackend.storage.entities.templating.pipeline;

import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "pipeline")

public class Pipeline {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pipelineId;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_wireframe_id")
    private Wireframe wireframe;
    @Column(columnDefinition = "smallint default 0")
    private Short currentStage;
    @OneToMany
    @JoinColumn(name = "f_pipeline_id")
    @BatchSize(size = 25)
    private List<PipelineItem> stages;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Task task;
}
