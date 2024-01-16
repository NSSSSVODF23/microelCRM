package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.task.utils.TaskTag;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.AlreadyExists;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.repositories.TaskTagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TaskTagDispatcher {
    private final TaskTagRepository taskTagRepository;

    public TaskTagDispatcher(TaskTagRepository taskTagRepository) {
        this.taskTagRepository = taskTagRepository;
    }

    public TaskTag create(TaskTag.Form form, Employee creator) throws AlreadyExists, IllegalFields {
        // Check if there is a tag with the same name
        TaskTag existingTag = taskTagRepository.findByName(form.getName());
        if(existingTag != null) throw new AlreadyExists();
        TaskTag taskTag = form.toTaskTag();
        taskTag.setCreator(creator);
        taskTag.setCreated(Timestamp.from(Instant.now()));
        taskTag.setDeleted(false);
        return taskTagRepository.save(taskTag);
    }
    public TaskTag modify(TaskTag.Form form) throws EntryNotFound, IllegalFields {
        if(form.getId() == null) throw new IllegalFields("Не указан id тега");
        TaskTag existingTag = taskTagRepository.findById(form.getId()).orElseThrow(()->new EntryNotFound("Тега не существует"));
        existingTag.setName(form.getName());
        existingTag.setColor(form.getColor());
        existingTag.setUnbindAfterClose(form.getUnbindAfterClose());
        existingTag.setDeleted(false);
        return taskTagRepository.save(existingTag);
    }
    public TaskTag delete(Long id) throws EntryNotFound {
        // We take a tag from the database for editing
        TaskTag existingTag = taskTagRepository.findById(id).orElseThrow(()-> new EntryNotFound("Данного тега не существует"));

        if(existingTag.getTask().isEmpty()){
            taskTagRepository.deleteById(id);
            return existingTag;
        }else {
            existingTag.setDeleted(true);
        }

        return taskTagRepository.save(existingTag);
    }
    public List<TaskTag> getAll(@Nullable String queryName,
                                @Nullable Boolean includingRemove) {
        return taskTagRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if(queryName != null && !queryName.isBlank()) predicates.add(cb.like(cb.lower(root.get("name")), "%" + queryName.toLowerCase() + "%"));
                    if(!(includingRemove != null && includingRemove)) predicates.add(cb.equal(root.get("deleted"), false));
                    return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Direction.ASC, "name"));
    }

    public boolean valid(Set<TaskTag> taskTags) {
        List<TaskTag> all = taskTagRepository.findAll((root,query,cb)->{
            List<Long> tagIds = taskTags.stream().map(TaskTag::getTaskTagId).collect(Collectors.toList());
            return root.get("taskTagId").in(tagIds);
        }, Sort.unsorted());
        return taskTags.size() == all.size();
    }

    public List<TaskTag> getByName(String query) {
        return taskTagRepository.findAllByNameContainingIgnoreCaseAndDeletedIsFalseOrderByName(query);
    }

    @Nullable
    public TaskTag get(Long id) {
        return taskTagRepository.findById(id).orElse(null);
    }
}
