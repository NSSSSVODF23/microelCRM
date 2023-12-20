package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.OldTrackerBind;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.fields.FieldDataBind;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.DefaultObserverRepository;
import com.microel.trackerbackend.storage.repositories.TaskStageRepository;
import com.microel.trackerbackend.storage.repositories.WireframeRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class WireframeDispatcher {
    private final WireframeRepository wireframeRepository;
    private final TaskStageRepository taskStageRepository;
    private final TaskDispatcher taskDispatcher;
    private final DefaultObserverRepository  defaultObserverRepository;


    public WireframeDispatcher(WireframeRepository wireframeRepository, TaskStageRepository taskStageRepository, @Lazy TaskDispatcher taskDispatcher, DefaultObserverRepository defaultObserverRepository) {
        this.wireframeRepository = wireframeRepository;
        this.taskStageRepository = taskStageRepository;
        this.taskDispatcher = taskDispatcher;
        this.defaultObserverRepository = defaultObserverRepository;
    }

    @Transactional
    public Wireframe createWireframe(Wireframe.Form form, Employee creator){
        Wireframe savedWireframe = wireframeRepository.save(Wireframe.builder()
                .creator(creator)
                .created(Timestamp.from(Instant.now()))
                .description(form.getDescription())
                .wireframeType(form.getWireframeType())
                .steps(form.getSteps())
                .deleted(false)
                .listViewType(form.getListViewType())
                .detailedViewType(form.getDetailedViewType())
                .stages(new HashSet<>())
                .documentTemplates(form.getDocumentTemplates().stream().map(DocumentTemplate.Form::toEntity).collect(Collectors.toList()))
                .name(form.getName())
                .build());

        for (TaskStage.Form stageForm : form.getStages()) {
            savedWireframe.getStages().add(stageForm.toEntity());
        }

        savedWireframe.setDefaultObservers(form.getDefaultObservers());
        return wireframeRepository.save(savedWireframe);
    }

    public List<Wireframe> getAllWireframes(Boolean includingDeleted) {
        if(!includingDeleted) return wireframeRepository.findAllByDeletedIsFalse(Sort.by(Sort.Direction.ASC, "name"));
        return wireframeRepository.findAll(Sort.by(Sort.Order.asc("deleted"), Sort.Order.asc("name")));
    }

    @Nullable
    public Wireframe getWireframeById(Long id) {
        return wireframeRepository.findById(id).orElse(null);
    }

    public Wireframe getWireframeById(Long id, Boolean isDeleted) {
        return wireframeRepository.findByWireframeIdAndDeleted(id, isDeleted).orElse(null);
    }

    @Transactional
    public Wireframe updateWireframe(Long id, Wireframe.Form form) {
        Wireframe founded = wireframeRepository.findById(id).orElse(null);
        if (founded == null) return null;
        founded.setName(form.getName());
        founded.setDescription(form.getDescription());
        founded.setSteps(form.getSteps());
        founded.setDefaultObservers(form.getDefaultObservers());
        founded.setListViewType(form.getListViewType());
        founded.setDetailedViewType(form.getDetailedViewType());

        if(founded.getDocumentTemplates() != null) {
            founded.getDocumentTemplates().removeIf(documentTemplate -> form.getDocumentTemplates().stream().map(DocumentTemplate.Form::getDocumentTemplateId).noneMatch(fdt -> Objects.equals(fdt, documentTemplate.getDocumentTemplateId())));
        }else{
            founded.setDocumentTemplates(new ArrayList<>());
        }

        if(form.getDocumentTemplates() != null)
            for(DocumentTemplate.Form documentTemplateForm : form.getDocumentTemplates()){
                DocumentTemplate existedDocumentTemplate = founded.getDocumentTemplates().stream().filter(documentTemplate -> Objects.equals(documentTemplate.getDocumentTemplateId(), documentTemplateForm.getDocumentTemplateId())).findFirst().orElse(null);
                if(existedDocumentTemplate != null){
                    existedDocumentTemplate.update(documentTemplateForm);
                }else{
                    founded.getDocumentTemplates().add(documentTemplateForm.toEntity());
                }
            }

        founded.getStages().removeIf(taskStage -> form.getStages().stream().map(TaskStage.Form::getStageId).noneMatch(fs-> Objects.equals(fs, taskStage.getStageId())));

        for (TaskStage.Form stageForm : form.getStages()) {
            TaskStage existedStage = founded.getStages().stream().filter(taskStage -> Objects.equals(taskStage.getStageId(), stageForm.getStageId())).findFirst().orElse(null);
            if(existedStage != null){
                existedStage.setLabel(stageForm.getLabel());
                existedStage.setOrderIndex(stageForm.getOrderIndex());
                OldTrackerBind existedOldTrackerBind = existedStage.getOldTrackerBind();
                if(existedOldTrackerBind != null){
                    existedOldTrackerBind.setClassId(stageForm.getOldTrackerBind().getClassId());
                    existedOldTrackerBind.setInitialStageId(stageForm.getOldTrackerBind().getInitialStageId());
                    existedOldTrackerBind.setProcessingStageId(stageForm.getOldTrackerBind().getProcessingStageId());
                    existedOldTrackerBind.setAutoCloseStageId(stageForm.getOldTrackerBind().getAutoCloseStageId());
                    existedOldTrackerBind.setManualCloseStageId(stageForm.getOldTrackerBind().getManualCloseStageId());
                    existedOldTrackerBind.setFieldDataBinds(stageForm.getOldTrackerBind().getFieldDataBinds().stream().map(FieldDataBind.Form::toEntity).collect(Collectors.toList()));
                }else if(stageForm.getOldTrackerBind() != null){
                    existedStage.setOldTrackerBind(stageForm.getOldTrackerBind().toEntity());
                }
            }else{
                founded.getStages().add(stageForm.toEntity());
            }
        }

        return wireframeRepository.save(founded);
    }

    @Transactional
    public Wireframe deleteWireframe(Long id) throws EntryNotFound {
        Wireframe wireframe = wireframeRepository.findById(id).orElseThrow(() -> new EntryNotFound("Шаблон не найден"));
        if(taskDispatcher.getCountByWireframe(wireframe) == 0L){
            wireframeRepository.delete(wireframe);
            return wireframe;
        }
        wireframe.setDeleted(true);
        return wireframeRepository.save(wireframe);
    }

    public boolean isStageExists(String id){
        return taskStageRepository.existsById(id);
    }

    public List<FilterModelItem> getFiltrationFields(Long wireframeId) {
        Wireframe wireframe = wireframeRepository.findById(wireframeId).orElse(null);
        if(wireframe == null) return new ArrayList<>();
        return wireframe.getAllFields().stream().map(FieldItem::toFilterModelItem).collect(Collectors.toList());
    }
}
