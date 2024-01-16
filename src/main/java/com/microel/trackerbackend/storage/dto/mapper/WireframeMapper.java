package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.templating.WireframeDto;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class WireframeMapper {
    @Nullable
    public static WireframeDto toDto(@Nullable Wireframe wireframe) {
        if (wireframe == null) return null;
        return WireframeDto.builder()
                .wireframeId(wireframe.getWireframeId())
                .wireframeType(wireframe.getWireframeType())
                .created(wireframe.getCreated())
                .creator(wireframe.getCreator())
                .defaultObservers(wireframe.getDefaultObservers() == null ? new ArrayList<>() : wireframe.getDefaultObservers().stream().map(DefaultObserverMapper::toDto).collect(Collectors.toList()))
                .deleted(wireframe.getDeleted())
                .description(wireframe.getDescription())
                .detailedViewType(wireframe.getDetailedViewType())
                .listViewType(wireframe.getListViewType())
                .name(wireframe.getName())
                .stages(wireframe.getStages() == null ? new HashSet<>() : wireframe.getStages().stream().map(TaskStageMapper::toDto).collect(Collectors.toSet()))
                .steps(wireframe.getSteps())
                .allFields(wireframe.getAllFields())
                .build();
    }

    @Nullable
    public static Wireframe fromDto(@Nullable WireframeDto wireframe) {
        if (wireframe == null) return null;
        return Wireframe.builder()
                .wireframeId(wireframe.getWireframeId())
                .wireframeType(wireframe.getWireframeType())
                .created(wireframe.getCreated())
                .creator(wireframe.getCreator())
                .defaultObservers(wireframe.getDefaultObservers() == null ? new ArrayList<>() : wireframe.getDefaultObservers().stream().map(DefaultObserverMapper::fromDto).collect(Collectors.toList()))
                .deleted(wireframe.getDeleted())
                .description(wireframe.getDescription())
                .detailedViewType(wireframe.getDetailedViewType())
                .listViewType(wireframe.getListViewType())
                .name(wireframe.getName())
                .stages(wireframe.getStages() == null ? new ArrayList<>() : wireframe.getStages().stream().map(TaskStageMapper::fromDto).collect(Collectors.toList()))
                .steps(wireframe.getSteps())
                .build();
    }
}
