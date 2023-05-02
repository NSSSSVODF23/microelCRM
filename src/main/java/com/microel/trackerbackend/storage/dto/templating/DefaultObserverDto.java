package com.microel.trackerbackend.storage.dto.templating;

import com.microel.trackerbackend.storage.entities.templating.DefaultObserverTargetType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DefaultObserverDto {
    private String targetId;
    private DefaultObserverTargetType targetType;
}
