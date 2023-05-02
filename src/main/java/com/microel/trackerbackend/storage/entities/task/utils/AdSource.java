package com.microel.trackerbackend.storage.entities.task.utils;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "ad_source")
public class AdSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adSourceId;
    private String name;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}
