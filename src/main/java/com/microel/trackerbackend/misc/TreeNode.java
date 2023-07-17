package com.microel.trackerbackend.misc;

import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.PaidWorkGroup;
import lombok.*;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode {
    private String key;
    private String label;
    private String icon;
    private Object data;
    private String type;
    private Boolean leaf;
    private Boolean draggable;
    private Boolean droppable;
    @Nullable
    private Integer position;
    private List<TreeNode> children;

    public Long getLongKey(){
        return Long.parseLong(key.substring(1));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MoveEvent {
        private List<Long> sourcePath;
        private List<Long> targetPath;
        private TreeNode object;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class UpdateEvent {
        private List<Long> path;
        private TreeNode object;
    }

    public static TreeNode from(PaidWorkGroup group, Boolean undraggable){
        return TreeNode.builder()
                .key("g"+group.getPaidWorkGroupId().toString())
                .label(group.getName())
                .icon("mdi-folder")
                .data(group)
                .type("group")
                .leaf(group.getChildrenGroups().isEmpty() && group.getPaidWorks().isEmpty())
                .draggable(!undraggable)
                .droppable(true)
                .position(group.getPosition())
                .children(new ArrayList<>())
                .build();
    }

    public static TreeNode from(PaidWork work){
        return TreeNode.builder()
                .key("w"+work.getPaidWorkId().toString())
                .label(work.getName())
                .icon("mdi-construction")
                .data(work)
                .type("work")
                .leaf(true)
                .droppable(false)
                .position(work.getPosition())
                .children(new ArrayList<>())
                .build();
    }
}
