package com.microel.trackerbackend.misc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.List;

@Getter
@Setter
public class TreeElementPosition {
    Long id;
    String type;
    Integer position;
    @Nullable
    List<Long> path;
}
