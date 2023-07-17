package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.salary.PaidAction;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EditingNotPossible;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.PaidActionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PaidActionDispatcher {
    private final PaidActionRepository paidActionRepository;
    private final PaidActionTemplateDispatcher paidActionTemplateDispatcher;
    private final StompController stompController;

    public PaidActionDispatcher(PaidActionRepository paidActionRepository, PaidActionTemplateDispatcher paidActionTemplateDispatcher, StompController stompController) {
        this.paidActionRepository = paidActionRepository;
        this.paidActionTemplateDispatcher = paidActionTemplateDispatcher;
        this.stompController = stompController;
    }

    public Page<PaidAction> getPage(Integer page, PaidAction.Filter filter){
        return paidActionRepository.findAll((root, query, cb)->{
            List<Predicate> predicates = new ArrayList<>();
            if(filter.getNameQuery() != null && !filter.getNameQuery().isBlank()){
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.getNameQuery().toLowerCase() + "%"));
            }
            if(filter.getIncludeDeleted() != null){
                if(!filter.getIncludeDeleted()){
                    predicates.add(cb.equal(root.get("deleted"), false));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(page, 25, Sort.by(Sort.Order.asc("deleted"), Sort.Order.asc("name"), Sort.Order.desc("created"))));
    }

    public void create(PaidAction.Form paidActionForm, Employee employee) throws AlreadyExists {
        if(!paidActionForm.fullFilled()){
            throw new IllegalFields("Не все поля заполнены");
        }

        if(paidActionRepository.existsByNameIgnoreCase(paidActionForm.getName())){
            throw new AlreadyExists("Платное действие с таким именем уже существует");
        }

        PaidAction paidAction = PaidAction.builder()
                .identifier(UUID.randomUUID())
                .name(paidActionForm.getName())
                .description(paidActionForm.getDescription())
                .unit(paidActionForm.getUnit())
                .cost(paidActionForm.getCost())
                .creator(employee)
                .created(Timestamp.from(Instant.now()))
                .deleted(false)
                .edited(false)
                .build();

        stompController.createPaidAction(paidActionRepository.save(paidAction));
    }

    public void edit(Long id, PaidAction.Form paidActionForm, Employee employee) throws IllegalFields, EditingNotPossible {
        if(!paidActionForm.fullFilled()){
            throw new IllegalFields("Не все поля заполнены");
        }

        PaidAction existedPaidAction = paidActionRepository.findByPaidActionIdAndDeletedFalse(id).orElseThrow(() -> new IllegalFields("Платного действие с таким идентификатором не существует"));

        if(paidActionForm.fullEqual(existedPaidAction)){
            throw new EditingNotPossible("Измените данные чтобы сохранить");
        }

        existedPaidAction.setDeleted(true);


        PaidAction newPaidAction = PaidAction.builder()
                .identifier(existedPaidAction.getIdentifier())
                .name(paidActionForm.getName())
                .description(paidActionForm.getDescription())
                .unit(paidActionForm.getUnit())
                .cost(paidActionForm.getCost())
                .creator(employee)
                .created(Timestamp.from(Instant.now()))
                .deleted(false)
                .edited(true)
                .build();
        paidActionRepository.save(existedPaidAction);
        PaidAction savedPaidAction = paidActionRepository.save(newPaidAction);
        stompController.updatePaidAction(savedPaidAction);
        paidActionTemplateDispatcher.replaceActualAction(existedPaidAction.getPaidActionId(), savedPaidAction);
    }

    public void delete(Long id, Employee employee) throws IllegalFields {
        PaidAction existedPaidAction = paidActionRepository.findByPaidActionIdAndDeletedFalse(id).orElseThrow(() -> new IllegalFields("Платного действие с таким идентификатором не существует"));
        existedPaidAction.setDeleted(true);
        stompController.deletePaidAction(paidActionRepository.save(existedPaidAction));
    }

    public List<PaidAction> getByIds(List<Long> ids) {
        return paidActionRepository.findAllById(ids);
    }

    public List<PaidAction> getAvailableList() {
        return paidActionRepository.findAllByDeletedFalseOrderByName();
    }

    public PaidAction get(Long actionId) {
        return paidActionRepository.findById(actionId).orElseThrow(() -> new EntryNotFound("Платного действия с таким идентификатором не существует"));
    }
}
