package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.TreeDragDropEvent;
import com.microel.trackerbackend.misc.TreeElementPosition;
import com.microel.trackerbackend.misc.TreeNode;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.salary.PaidActionTemplate;
import com.microel.trackerbackend.storage.entities.salary.PaidWork;
import com.microel.trackerbackend.storage.entities.salary.PaidWorkGroup;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.PaidWorkRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PaidWorkDispatcher {
    private final PaidWorkRepository paidWorkRepository;
    private final PaidWorkGroupDispatcher paidWorkGroupDispatcher;
    private final PaidActionDispatcher paidActionDispatcher;
    private final StompController stompController;

    public PaidWorkDispatcher(PaidWorkRepository paidWorkRepository, @Lazy PaidWorkGroupDispatcher paidWorkGroupDispatcher, PaidActionDispatcher paidActionDispatcher, StompController stompController) {
        this.paidWorkRepository = paidWorkRepository;
        this.paidWorkGroupDispatcher = paidWorkGroupDispatcher;
        this.paidActionDispatcher = paidActionDispatcher;
        this.stompController = stompController;
    }

    public List<PaidWork> getRootTree() {
        return paidWorkRepository.findAll(
                (root, query, cb) -> cb.isNull(root.get("parentGroup")),
                Sort.by(Sort.Order.asc("position"))
        );
    }

    public List<PaidWork> getInGroup(Long groupId) {
        return paidWorkRepository.findAll(
                (root, query, cb) -> cb.equal(root.join("parentGroup").get("paidWorkGroupId"), groupId),
                Sort.by(Sort.Order.asc("position"))
        );
    }

    public void create(PaidWork.Form form, Employee employee) {
        if(!form.isValid()) throw new IllegalFields("Не все поля заполнены");
        PaidWork paidWork = PaidWork.builder()
                .name(form.getName())
                .description(form.getDescription())
                .parentGroup(form.getParentGroupId() == null ? null : paidWorkGroupDispatcher.getById(form.getParentGroupId()))
                .actions(new ArrayList<>())
                .build();
        List<PaidAction> actionsFromDB = paidActionDispatcher.getByIds(form.getActions().stream().map(PaidActionTemplate.Form::getActionId).toList());
        for(PaidActionTemplate.Form action : form.getActions()) {
            PaidAction target = actionsFromDB.stream().filter(a->a.getPaidActionId().equals(action.getActionId())).findFirst().orElseThrow(() -> new EntryNotFound("Действие не найдено в базе данных"));
            paidWork.getActions().add(PaidActionTemplate.builder().action(target).count(action.getCount()).build());
        }
        PaidWork work = paidWorkRepository.save(paidWork);
        stompController.createPaidWorksTreeItem(new TreeNode.UpdateEvent(work.getPath(), TreeNode.from(work)));
    }

    public void edit(Long id, PaidWork.Form form, Employee employee) {
        if(!form.isValid()) throw new IllegalFields("Не все поля заполнены");
        PaidWork existing = paidWorkRepository.findById(id).orElseThrow(() -> new EntryNotFound("Работа не найдена"));
        existing.setName(form.getName());
        existing.setDescription(form.getDescription());
        List<PaidAction> actionsFromDB = paidActionDispatcher.getByIds(form.getActions().stream().map(PaidActionTemplate.Form::getActionId).toList());
        for(PaidActionTemplate.Form action : form.getActions()) {
            PaidActionTemplate paidActionTemplate = existing.getActions().stream().filter(a -> a.getAction().getPaidActionId().equals(action.getActionId())).findFirst().orElse(null);
            if(paidActionTemplate != null){
                paidActionTemplate.setCount(action.getCount());
            }else{
                PaidAction target = actionsFromDB.stream().filter(a->a.getPaidActionId().equals(action.getActionId())).findFirst().orElseThrow(() -> new EntryNotFound("Действие не найдено в базе данных"));
                existing.getActions().add(PaidActionTemplate.builder().action(target).count(action.getCount()).build());
            }
        }
        existing.getActions().removeIf(ea -> form.getActions().stream().noneMatch(af -> af.getActionId().equals(ea.getAction().getPaidActionId())));
        PaidWork work = paidWorkRepository.save(existing);
        stompController.updatePaidWorksTreeItem(new TreeNode.UpdateEvent(work.getPath(), TreeNode.from(work)));
        stompController.updatePaidWork(work);
    }

    public void delete(Long id, Employee employee) {
        PaidWork work = paidWorkRepository.findById(id).orElseThrow(() -> new EntryNotFound("Работа не найдена"));
        stompController.deletePaidWorksTreeItem(new TreeNode.UpdateEvent(work.getPath(), TreeNode.from(work)));
        paidWorkRepository.delete(work);
    }

    public void dragDrop(TreeDragDropEvent event) {
        long sourceId = event.getSource().getLongKey();
        PaidWork source = paidWorkRepository.findById(sourceId).orElseThrow(() -> new EntryNotFound("Работа не найдена"));
        List<Long> from = source.getPath();
        if(event.hasTarget()){
            long targetId = event.getTarget().getLongKey();
            PaidWorkGroup target = paidWorkGroupDispatcher.getById(targetId);
            source.setParentGroup(target);
//            source.setPosition(event.getIndex());
            paidWorkRepository.save(source);
            List<Long> to = source.getPath();
            stompController.movePaidWorksTreeItem(new TreeNode.MoveEvent(from, to, TreeNode.from(source)));
        }else{
            source.setParentGroup(null);
//            source.setPosition(event.getIndex());
            paidWorkRepository.save(source);
            stompController.movePaidWorksTreeItem(new TreeNode.MoveEvent(from, new ArrayList<>(), TreeNode.from(source)));
        }
    }

    public PaidWork get(Long id) {
        return paidWorkRepository.findById(id).orElseThrow(() -> new EntryNotFound("Работа не найдена"));
    }

    public List<Long> reposition(TreeElementPosition position, Employee employee) {
        PaidWork work = get(position.getId());
        work.setPosition(position.getPosition());
        return paidWorkRepository.save(work).getPath();
    }
}
