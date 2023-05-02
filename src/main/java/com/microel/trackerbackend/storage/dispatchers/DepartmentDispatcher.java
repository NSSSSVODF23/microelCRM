package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.team.util.Department;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.DepartmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class DepartmentDispatcher {
    private final DepartmentRepository departmentRepository;

    public DepartmentDispatcher(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> getAll() {
        return departmentRepository.findAll((root, query, cb) -> cb.and(cb.equal(root.get("deleted"), false)), Sort.by(Sort.Direction.ASC, "name"));
    }

    public Department create(String name, String description) {
        return departmentRepository.save(
                Department.builder()
                        .name(name)
                        .description(description)
                        .created(Timestamp.from(Instant.now()))
                        .deleted(false)
                        .build()
        );
    }

    public Department edit(Long id, String name, String description) throws EntryNotFound {
        Department foundDepartment = departmentRepository.findById(id).orElse(null);
        if (foundDepartment == null) throw new EntryNotFound();
        foundDepartment.setName(name);
        foundDepartment.setDescription(description);
        return departmentRepository.save(foundDepartment);
    }

    public Department delete(Long id) throws EntryNotFound {
        Department foundDepartment = departmentRepository.findById(id).orElse(null);
        if (foundDepartment == null) throw new EntryNotFound();
        foundDepartment.setDeleted(true);
        return departmentRepository.save(foundDepartment);
    }

    @Nullable
    public Department getById(@Nullable Long id) throws EntryNotFound {
        if(id == null) return null;
        Department department = departmentRepository.findById(id).orElse(null);
        if (department == null) throw new EntryNotFound("Отдел с идентификатором " + id + "не найден в базе данных");
        return department;
    }

    public List<Department> getByIdSet(Set<Long> departmentResponsibilities) {
        return departmentRepository.findAllByDepartmentIdIsInAndDeletedIsFalse(departmentResponsibilities);
    }

    public Department get(Long id) throws EntryNotFound {
        Department department = departmentRepository.findById(id).orElse(null);
        if(department == null) throw new EntryNotFound("Отдел не найден");
        return department;
    }
}
