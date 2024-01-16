package com.microel.trackerbackend.storage.dto.task;

import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WireframeListDto {
    private Long wireframeId;
    private String name;
    private String listViewType;
    private List<FieldItem> allFields;

    @Nullable
    public static WireframeListDto of(@Nullable Wireframe wireframe) {
        if(wireframe == null) return null;
        return WireframeListDto.builder()
                .wireframeId(wireframe.getWireframeId())
                .name(wireframe.getName())
                .listViewType(wireframe.getListViewType())
                .allFields(wireframe.getAllFields())
                .build();
    }
}
