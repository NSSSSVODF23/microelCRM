package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.TaskStatus;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Optional<Task> findByTaskId(Long id);

    @Query(value = "select * from public.tasks LIMIT 500"
            , nativeQuery = true)
    Set<Task> getTasksTest();

    Long countByModelWireframe(Wireframe wireframe);

    Long countByModelWireframe_WireframeIdAndDeletedFalseAndTaskStatusNot(Long wireframeId, TaskStatus close);

    Long countByModelWireframe_WireframeIdAndDeletedFalseAndTaskStatus(Long wireframeId, TaskStatus taskStatus);

    List<Task> findByCurrentDirectory_TaskTypeDirectoryIdIn(List<Long> taskTypeDirectoriesIds);
}
