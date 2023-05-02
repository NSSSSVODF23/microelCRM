package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.team.util.Position;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.PositionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class PositionDispatcher {
    private final PositionRepository positionRepository;

    public PositionDispatcher(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public List<Position> getAll() {
        return positionRepository.findAll(
                (root,query,cb)-> cb.and(cb.equal(root.get("deleted"), false)),
                Sort.by(Sort.Direction.ASC, "name")
        );
    }

    public Position create(String name, String description, Integer access) {
        Position.PositionBuilder positionBuilder = Position.builder();

        if (access == null || access < 0)
            positionBuilder.access(0);
        else
            positionBuilder.access(access);

        return positionRepository.save(
                positionBuilder
                        .name(name)
                        .description(description)
                        .created(Timestamp.from(Instant.now()))
                        .deleted(false)
                        .build()
        );
    }

    public Position edit(Long id, String name, String description, Integer access) throws EntryNotFound {
        Position foundPosition = positionRepository.findById(id).orElse(null);
        if (foundPosition == null) throw new EntryNotFound();
        foundPosition.setName(name);
        foundPosition.setDescription(description);

        if (access == null || access < 0)
            foundPosition.setAccess(0);
        else
            foundPosition.setAccess(access);

        return positionRepository.save(foundPosition);
    }

    public Position delete(Long id) throws EntryNotFound {
        Position foundedPosition = positionRepository.findById(id).orElse(null);
        if (foundedPosition == null) throw new EntryNotFound();
        foundedPosition.setDeleted(true);
        return positionRepository.save(foundedPosition);
    }

    @Nullable
    public Position getById(@Nullable Long id) throws EntryNotFound {
        if(id == null) return null;
        Position position = positionRepository.findById(id).orElse(null);
        if (position == null)
            throw new EntryNotFound("Должность с идентификатором " + id + " не найдена в базе данных");
        return position;
    }
}
