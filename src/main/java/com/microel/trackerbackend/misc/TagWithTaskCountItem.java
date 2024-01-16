package com.microel.trackerbackend.misc;

import lombok.Data;
import lombok.NonNull;

@Data
public class TagWithTaskCountItem {
    @NonNull
    private Long id;
    @NonNull
    private String name;
    @NonNull
    private String color;
    @NonNull
    private Long count;
}
