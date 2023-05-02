package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.team.PositionDto;
import com.microel.trackerbackend.storage.entities.team.util.Position;
import org.springframework.lang.Nullable;

public class PositionMapper {
    @Nullable
    public static PositionDto toDto(@Nullable Position position) {
        if (position == null) return null;
        return PositionDto.builder()
                .positionId(position.getPositionId())
                .name(position.getName())
                .access(position.getAccess())
                .created(position.getCreated())
                .description(position.getDescription())
                .deleted(position.getDeleted())
                .build();
    }

    @Nullable
    public static Position fromDto(@Nullable PositionDto position) {
        if (position == null) return null;
        return Position.builder()
                .positionId(position.getPositionId())
                .name(position.getName())
                .access(position.getAccess())
                .created(position.getCreated())
                .description(position.getDescription())
                .deleted(position.getDeleted())
                .build();
    }
}
