package com.microel.trackerbackend.storage.entities.templating.pipeline;

import com.microel.trackerbackend.storage.entities.templating.WireframeFieldType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "pipelines_items")
public class PipelineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pipelineItemId;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_parent_pipeline_id")
    private Pipeline parentPipeline;
    private Short index;
    @Column(length = 48)
    private String innerName;
    @Column(length = 48)
    private String name;
    @Column(length = 512)
    private String description;
    @Enumerated(EnumType.STRING)
    private WireframeFieldType wireframeFieldType;
    @Column(columnDefinition = "text")
    private String data;
    private Timestamp processed;
    @Column(columnDefinition = "boolean default false")
    private Boolean skip;
}
