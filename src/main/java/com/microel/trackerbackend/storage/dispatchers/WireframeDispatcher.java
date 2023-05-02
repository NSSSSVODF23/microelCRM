package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.dto.mapper.WireframeMapper;
import com.microel.trackerbackend.storage.dto.templating.WireframeDto;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.templating.TaskStage;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.TaskStageRepository;
import com.microel.trackerbackend.storage.repositories.WireframeRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class WireframeDispatcher {
    private final WireframeRepository wireframeRepository;
    private final TaskStageRepository taskStageRepository;
    private final TaskDispatcher taskDispatcher;


    public WireframeDispatcher(WireframeRepository wireframeRepository, TaskStageRepository taskStageRepository, @Lazy TaskDispatcher taskDispatcher) {
        this.wireframeRepository = wireframeRepository;
        this.taskStageRepository = taskStageRepository;
        this.taskDispatcher = taskDispatcher;
    }

    public Wireframe createWireframe(Wireframe wireframe, Employee creator){
        return wireframeRepository.save(Wireframe.builder()
                        .creator(creator)
                        .created(Timestamp.from(Instant.now()))
                        .description(wireframe.getDescription())
                        .wireframeType(wireframe.getWireframeType())
                        .steps(wireframe.getSteps())
                        .defaultObservers(wireframe.getDefaultObservers())
                        .deleted(false)
                        .listViewType(wireframe.getListViewType())
                        .detailedViewType(wireframe.getDetailedViewType())
                        .stages(wireframe.getStages())
                        .name(wireframe.getName())
                .build());
    }

    public List<Wireframe> getAllWireframes(Boolean includingDeleted) {
        if(includingDeleted) return wireframeRepository.findAllByDeletedIsFalse(Sort.by(Sort.Direction.ASC, "name"));
        return wireframeRepository.findAll(Sort.by(Sort.Direction.DESC,"name"));
    }

    @Nullable
    public Wireframe getWireframeById(Long id) {
        return wireframeRepository.findById(id).orElse(null);
    }

    public Wireframe getWireframeById(Long id, Boolean isDeleted) {
        return wireframeRepository.findByWireframeIdAndDeleted(id, isDeleted).orElse(null);
    }

    public Object updateWireframe(Wireframe wireframe) {
        Wireframe founded = wireframeRepository.findById(wireframe.getWireframeId()).orElse(null);
        if (founded == null) return null;
        founded.setName(wireframe.getName());
        founded.setDescription(wireframe.getDescription());
        founded.setSteps(wireframe.getSteps());
        founded.setDefaultObservers(wireframe.getDefaultObservers());
        founded.setListViewType(wireframe.getListViewType());
        founded.setDetailedViewType(wireframe.getDetailedViewType());
        founded.setStages(wireframe.getStages());
        return wireframeRepository.save(founded);
    }

    public void deleteWireframe(Long id) throws EntryNotFound {
        Wireframe wireframe = wireframeRepository.findById(id).orElseThrow(() -> new EntryNotFound("Шаблон не найден"));
        if(taskDispatcher.getCountByWireframe(wireframe) > 0L){
            wireframeRepository.delete(wireframe);
            return;
        }
        wireframe.setDeleted(true);
        wireframeRepository.save(wireframe);
    }


    public boolean isStageExists(String id){
        return taskStageRepository.existsById(id);
    }
}
