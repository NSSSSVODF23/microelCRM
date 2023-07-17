package com.microel.trackerbackend.misc;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TreeDragDropEvent {
    private TreeNode source;
    private TreeNode target;
    private Integer index;

    public boolean hasSource(){
        return source != null;
    }

    public boolean hasTarget(){
        return target != null;
    }
}
