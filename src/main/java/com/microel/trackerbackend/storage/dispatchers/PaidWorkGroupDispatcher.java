package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.TreeDragDropEvent;
import com.microel.trackerbackend.misc.TreeElementPosition;
import com.microel.trackerbackend.misc.TreeNode;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.PaidWorkGroup;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.PaidWorkGroupRepository;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class PaidWorkGroupDispatcher {
    private final PaidWorkGroupRepository paidWorkGroupRepository;
    private final PaidWorkDispatcher paidWorkDispatcher;
    private final StompController stompController;

    public PaidWorkGroupDispatcher(PaidWorkGroupRepository paidWorkGroupRepository, PaidWorkDispatcher paidWorkDispatcher, StompController stompController) {
        this.paidWorkGroupRepository = paidWorkGroupRepository;
        this.paidWorkDispatcher = paidWorkDispatcher;
        this.stompController = stompController;
    }

    public List<TreeNode> getRootTree(@Nullable Boolean undraggable) {
        List<TreeNode> treeNodes = new ArrayList<>();

        List<PaidWorkGroup> foundGroups = paidWorkGroupRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("isRoot"), true),
                Sort.by(Sort.Order.asc("position"))
        );
        List<PaidWork> foundWorks = paidWorkDispatcher.getRootTree();


        for(PaidWorkGroup workGroup : foundGroups){
            treeNodes.add(TreeNode.from(workGroup, Boolean.TRUE.equals(undraggable)));
        }

        for(PaidWork work : foundWorks){
            treeNodes.add(TreeNode.from(work));
        }

        treeNodes.sort(Comparator.comparing(TreeNode::getPosition, Comparator.nullsLast(Comparator.naturalOrder())));

        return treeNodes;
    }

    public List<TreeNode> getTree(Long groupId, @Nullable Boolean undraggable) {
        List<TreeNode> treeNodes = new ArrayList<>();
        PaidWorkGroup foundGroup = paidWorkGroupRepository.findById(groupId).orElseThrow(() -> new EntryNotFound("Группа не найдена"));
        List<PaidWork> foundWorks = paidWorkDispatcher.getInGroup(groupId);
        for(PaidWorkGroup group: foundGroup.getChildrenGroups()){
            treeNodes.add(TreeNode.from(group, Boolean.TRUE.equals(undraggable)));
        }
        for(PaidWork work: foundWorks){
            treeNodes.add(TreeNode.from(work));
        }
        treeNodes.sort(Comparator.comparing(TreeNode::getPosition,Comparator.nullsLast(Comparator.naturalOrder())));
        return treeNodes;
    }

    public void create(PaidWorkGroup.Form form, Employee employee) throws EntryNotFound, IllegalFields {
        if (!form.isValid()) throw new IllegalFields("Не все поля заполнены");
        PaidWorkGroup paidWorkGroup = PaidWorkGroup.builder()
                .name(form.getName())
                .description(form.getDescription())
                .childrenGroups(new ArrayList<>())
                .paidWorks(new ArrayList<>())
                .isRoot(form.getParentGroupId() == null)
                .build();

        PaidWorkGroup parent = null;
        if (form.getParentGroupId() != null) {
            parent = paidWorkGroupRepository.findById(form.getParentGroupId()).orElseThrow(() -> new EntryNotFound("Группа не найдена"));
        }

        paidWorkGroupRepository.save(paidWorkGroup);
        if (parent != null) {
            parent.getChildrenGroups().add(paidWorkGroup);
            paidWorkGroup.setParentGroup(parent);
            paidWorkGroupRepository.save(parent);
        }

        stompController.createPaidWorksTreeItem(new TreeNode.UpdateEvent(paidWorkGroup.getPath(), TreeNode.from(paidWorkGroup, false)));
    }

    public void edit(Long id, PaidWorkGroup.Form form, Employee employee) throws EntryNotFound, IllegalFields {
        if (!form.isValid()) throw new IllegalFields("Не все поля заполнены");
        PaidWorkGroup paidWorkGroup = paidWorkGroupRepository.findById(id).orElseThrow(() -> new EntryNotFound("Группа не найдена"));
        paidWorkGroup.setName(form.getName());
        paidWorkGroup.setDescription(form.getDescription());
        paidWorkGroupRepository.save(paidWorkGroup);
        stompController.updatePaidWorkGroup(paidWorkGroup);
        stompController.updatePaidWorksTreeItem(new TreeNode.UpdateEvent(paidWorkGroup.getPath(), TreeNode.from(paidWorkGroup, false)));
    }

    public void delete(Long id, Employee employee) {
        PaidWorkGroup workGroup = paidWorkGroupRepository.findById(id).orElseThrow(()-> new EntryNotFound("Группа не найдена"));
        if(!workGroup.getPaidWorks().isEmpty()){
            PaidWorkGroup parentGroup = workGroup.getParentGroup();
            if(parentGroup != null){
                List<PaidWork> paidWorks = workGroup.getPaidWorks();
                parentGroup.setPaidWorks(paidWorks);
                parentGroup.getChildrenGroups().remove(workGroup);
                paidWorks.forEach(paidWork -> stompController.movePaidWorksTreeItem(new TreeNode.MoveEvent(workGroup.getPath(), workGroup.getPath(), TreeNode.from(paidWork))));
                stompController.deletePaidWorksTreeItem(new TreeNode.UpdateEvent(workGroup.getPath(), TreeNode.from(workGroup, false)));
                workGroup.clearPaidWorks();
                paidWorkGroupRepository.delete(workGroup);
                paidWorkGroupRepository.save(parentGroup);
            }else{
                List<PaidWork> paidWorks = workGroup.getPaidWorks();
                paidWorks.forEach(paidWork -> stompController.movePaidWorksTreeItem(new TreeNode.MoveEvent(workGroup.getPath(), new ArrayList<>(), TreeNode.from(paidWork))));
                stompController.deletePaidWorksTreeItem(new TreeNode.UpdateEvent(new ArrayList<>(), TreeNode.from(workGroup, false)));
                workGroup.clearPaidWorks();
                paidWorkGroupRepository.delete(workGroup);
            }
        }else{
            stompController.deletePaidWorksTreeItem(new TreeNode.UpdateEvent(new ArrayList<>(), TreeNode.from(workGroup, false)));
            paidWorkGroupRepository.delete(workGroup);
        }
    }

    public void dragDrop(TreeDragDropEvent event) {
        long sourceId = event.getSource().getLongKey();
        PaidWorkGroup source = paidWorkGroupRepository.findById(sourceId).orElseThrow(() -> new EntryNotFound("Группа не найдена"));
        List<Long> from = source.getPath();
        if(event.hasTarget()){
            long targetId = event.getTarget().getLongKey();
            PaidWorkGroup target = paidWorkGroupRepository.findById(targetId).orElseThrow(() -> new EntryNotFound("Группа не найдена"));
            target.getChildrenGroups().add(source);
            source.setParentGroup(target);
            source.setRoot(false);
//            source.setPosition(event.getIndex());
            paidWorkGroupRepository.save(source);
            List<Long> to = source.getPath();
            stompController.movePaidWorksTreeItem(new TreeNode.MoveEvent(from, to, TreeNode.from(source, false)));
        }else{
            source.setParentGroup(null);
            source.setRoot(true);
//            source.setPosition(event.getIndex());
            paidWorkGroupRepository.save(source);
            stompController.movePaidWorksTreeItem(new TreeNode.MoveEvent(from, new ArrayList<>(), TreeNode.from(source, false)));
        }
    }

    public PaidWorkGroup getById(Long parentGroupId) {
        return paidWorkGroupRepository.findById(parentGroupId).orElseThrow(() -> new EntryNotFound("Группа не найдена"));
    }

    public List<Long> reposition(TreeElementPosition position, Employee employee) {
        PaidWorkGroup workGroup = paidWorkGroupRepository.findById(position.getId()).orElseThrow(() -> new EntryNotFound("Группа не найдена"));
        workGroup.setPosition(position.getPosition());
        return paidWorkGroupRepository.save(workGroup).getPath();
    }
}
