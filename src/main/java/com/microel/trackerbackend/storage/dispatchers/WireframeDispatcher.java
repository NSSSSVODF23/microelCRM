package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.DefaultObserver;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.TaskTypeDirectory;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.entities.templating.documents.DocumentTemplate;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FieldItem;
import com.microel.trackerbackend.storage.entities.templating.model.dto.FilterModelItem;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.OldTrackerBind;
import com.microel.trackerbackend.storage.entities.templating.oldtracker.fields.FieldDataBind;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class WireframeDispatcher {
    private final WireframeRepository wireframeRepository;
    private final TaskStageRepository taskStageRepository;
    private final TaskTypeDirectoryRepository taskTypeDirectoryRepository;
    private final TaskRepository taskRepository;
    private final DefaultObserverRepository defaultObserverRepository;


    public WireframeDispatcher(WireframeRepository wireframeRepository, TaskStageRepository taskStageRepository,
                               TaskTypeDirectoryRepository taskTypeDirectoryRepository, TaskRepository taskRepository,
                               DefaultObserverRepository defaultObserverRepository) {
        this.wireframeRepository = wireframeRepository;
        this.taskStageRepository = taskStageRepository;
        this.taskTypeDirectoryRepository = taskTypeDirectoryRepository;
        this.taskRepository = taskRepository;
        this.defaultObserverRepository = defaultObserverRepository;
    }

    @Transactional
    public Wireframe createWireframe(Wireframe.Form form, Employee creator) {
        Wireframe savedWireframe = wireframeRepository.save(Wireframe.builder()
                .creator(creator)
                .created(Timestamp.from(Instant.now()))
                .description(form.getDescription())
                .wireframeType(form.getWireframeType())
                .steps(form.getSteps())
                .deleted(false)
                .listViewType(form.getListViewType())
                .detailedViewType(form.getDetailedViewType())
                .stages(form.getStages().stream().map(TaskStage.Form::toEntity).toList())
                .documentTemplates(form.getDocumentTemplates().stream().map(DocumentTemplate.Form::toEntity).collect(Collectors.toList()))
                .name(form.getName())
                .build());

        for (TaskStage.Form stageForm : form.getStages()) {
            savedWireframe.getStages().add(stageForm.toEntity());
        }

        replaceExistedObservers(form.getDefaultObservers());
        savedWireframe.setDefaultObservers(form.getDefaultObservers());

        return wireframeRepository.save(savedWireframe);
    }

    public List<Wireframe> getAllWireframes(Boolean includingDeleted) {
        if (!includingDeleted) return wireframeRepository.findAllByDeletedIsFalse(Sort.by(Sort.Direction.ASC, "name"));
        return wireframeRepository.findAll(Sort.by(Sort.Order.asc("deleted"), Sort.Order.asc("name")));
    }

    @Nullable
    public Wireframe getWireframeById(Long id) {
        return wireframeRepository.findById(id).orElse(null);
    }

    public Wireframe getWireframeById(Long id, Boolean isDeleted) {
        return wireframeRepository.findByWireframeIdAndDeleted(id, isDeleted).orElse(null);
    }

    public List<DefaultObserver> replaceExistedObservers(List<DefaultObserver> targetList) {
        List<DefaultObserver> existedObservers = defaultObserverRepository.findAll((root, query, cb) ->
                root.get("targetId").in(targetList.stream().map(DefaultObserver::getTargetId).toList())
        );
        // Replace existed observers in targetList
        for (DefaultObserver existedObserver : existedObservers) {
            targetList.removeIf(target -> target.getTargetId().equals(existedObserver.getTargetId()));
            targetList.add(existedObserver);
        }
        return targetList;
    }

    @Transactional
    public Wireframe updateWireframe(Long id, Wireframe.Form form) {
        Wireframe founded = wireframeRepository.findById(id).orElse(null);
        if (founded == null) return null;
        founded.setName(form.getName());
        founded.setDescription(form.getDescription());
        founded.setSteps(form.getSteps());

        replaceExistedObservers(form.getDefaultObservers());
        founded.setDefaultObservers(form.getDefaultObservers());

        founded.setListViewType(form.getListViewType());
        founded.setDetailedViewType(form.getDetailedViewType());

        founded.clearRemovedStages(form.getStages());


        if (founded.getDocumentTemplates() != null) {
            founded.getDocumentTemplates().removeIf(documentTemplate -> form.getDocumentTemplates().stream().map(DocumentTemplate.Form::getDocumentTemplateId).noneMatch(fdt -> Objects.equals(fdt, documentTemplate.getDocumentTemplateId())));
        } else {
            founded.setDocumentTemplates(new ArrayList<>());
        }

        if (form.getDocumentTemplates() != null) {
            for (DocumentTemplate.Form documentTemplateForm : form.getDocumentTemplates()) {
                DocumentTemplate existedDocumentTemplate = founded.getDocumentTemplates().stream().filter(documentTemplate -> Objects.equals(documentTemplate.getDocumentTemplateId(), documentTemplateForm.getDocumentTemplateId())).findFirst().orElse(null);
                if (existedDocumentTemplate != null) {
                    existedDocumentTemplate.update(documentTemplateForm);
                } else {
                    founded.getDocumentTemplates().add(documentTemplateForm.toEntity());
                }
            }
        }

        for (TaskStage.Form stageForm : form.getStages()) {
            TaskStage existedStage = founded.getStages().stream().filter(taskStage -> Objects.equals(taskStage.getStageId(), stageForm.getStageId())).findFirst().orElse(null);
            if (existedStage != null) {
                if (stageForm.getDirectories() != null) {
                    List<TaskTypeDirectory> removingDirectories = existedStage
                            .getDirectories()
                            .stream()
                            .filter(dir -> stageForm.getDirectories()
                                    .stream().map(TaskTypeDirectory.Form::toEntity)
                                    .noneMatch(formDir -> Objects.equals(formDir.getTaskTypeDirectoryId(), dir.getTaskTypeDirectoryId()))
                            ).toList();
                    if (!removingDirectories.isEmpty()) {
//                        List<Long> dirIdList = removingDirectories.stream().map(TaskTypeDirectory::getTaskTypeDirectoryId).toList();
//                        List<Task> tasks = taskRepository.findAll((root, query, cb) -> cb.and(
//                                root.join("currentDirectory")
//                                        .get("taskTypeDirectoryId")
//                                        .in(dirIdList)
//                        ));
//                        tasks.forEach(task -> task.setCurrentDirectory(null));
//                        taskRepository.saveAll(tasks);
//                        taskTypeDirectoryRepository.deleteAll(removingDirectories);
                        existedStage.clearRemovedDirectories(removingDirectories);
                    }
                    for (TaskTypeDirectory.Form taskDirForm : stageForm.getDirectories()) {
                        TaskTypeDirectory existedTaskTypeDirectory = null;
                        if (taskDirForm.getTaskTypeDirectoryId() != null)
                            existedTaskTypeDirectory = existedStage.getDirectories().stream().filter(taskTypeDirectory -> Objects.equals(taskTypeDirectory.getTaskTypeDirectoryId(), taskDirForm.toEntity().getTaskTypeDirectoryId())).findFirst().orElse(null);
                        if (existedTaskTypeDirectory != null) {
                            existedTaskTypeDirectory.update(taskDirForm);
                        } else {
                            existedStage.appendDirectory(taskDirForm.toEntity());
                        }
                    }
                } else {
                    existedStage.setDirectories(new ArrayList<>());
                }

                existedStage.setLabel(stageForm.getLabel());
                existedStage.setOrderIndex(stageForm.getOrderIndex());
                OldTrackerBind existedOldTrackerBind = existedStage.getOldTrackerBind();
                if (existedOldTrackerBind != null) {
                    existedOldTrackerBind.setClassId(stageForm.getOldTrackerBind().getClassId());
                    existedOldTrackerBind.setInitialStageId(stageForm.getOldTrackerBind().getInitialStageId());
                    existedOldTrackerBind.setProcessingStageId(stageForm.getOldTrackerBind().getProcessingStageId());
                    existedOldTrackerBind.setAutoCloseStageId(stageForm.getOldTrackerBind().getAutoCloseStageId());
                    existedOldTrackerBind.setManualCloseStageId(stageForm.getOldTrackerBind().getManualCloseStageId());
                    existedOldTrackerBind.setFieldDataBinds(stageForm.getOldTrackerBind().getFieldDataBinds().stream().map(FieldDataBind.Form::toEntity).collect(Collectors.toList()));
                } else if (stageForm.getOldTrackerBind() != null) {
                    existedStage.setOldTrackerBind(stageForm.getOldTrackerBind().toEntity());
                }
            } else {
                founded.appendStage(stageForm.toEntity());
            }
        }

        return wireframeRepository.save(founded);
    }

    @Transactional
    public Wireframe deleteWireframe(Long id) throws EntryNotFound {
        Wireframe wireframe = wireframeRepository.findById(id).orElseThrow(() -> new EntryNotFound("Шаблон не найден"));
        if (taskRepository.countByModelWireframe(wireframe) == 0L) {
            wireframeRepository.delete(wireframe);
            return wireframe;
        }
        wireframe.setDeleted(true);
        return wireframeRepository.save(wireframe);
    }

    public boolean isStageExists(String id) {
        return taskStageRepository.existsById(id);
    }

    public List<FilterModelItem> getFiltrationFields(Long wireframeId) {
        Wireframe wireframe = wireframeRepository.findById(wireframeId).orElse(null);
        if (wireframe == null) return new ArrayList<>();
        return wireframe.getAllFields().stream().map(FieldItem::toFilterModelItem).collect(Collectors.toList());
    }
}
