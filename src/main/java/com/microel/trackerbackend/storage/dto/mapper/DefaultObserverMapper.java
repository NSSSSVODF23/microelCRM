package com.microel.trackerbackend.storage.dto.mapper;

import com.microel.trackerbackend.storage.dto.templating.DefaultObserverDto;
import com.microel.trackerbackend.storage.entities.templating.DefaultObserver;
import org.springframework.lang.Nullable;

public class DefaultObserverMapper {
    @Nullable
    public static DefaultObserverDto toDto(@Nullable DefaultObserver defaultObserver) {
        if (defaultObserver == null) return null;
        return DefaultObserverDto.builder()
                .targetId(defaultObserver.getTargetId())
                .name(defaultObserver.getName())
                .targetType(defaultObserver.getTargetType())
                .build();
    }

    @Nullable
    public static DefaultObserver fromDto(@Nullable DefaultObserverDto defaultObserver) {
        if (defaultObserver == null) return null;
        return DefaultObserver.builder()
                .targetId(defaultObserver.getTargetId())
                .name(defaultObserver.getName())
                .targetType(defaultObserver.getTargetType())
                .build();
    }
}
