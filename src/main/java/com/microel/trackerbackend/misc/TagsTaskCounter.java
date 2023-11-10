package com.microel.trackerbackend.misc;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TagsTaskCounter {
    private Long tagId;
    private Map<Long, Long> wireframeTask;
}
