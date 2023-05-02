package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.dto.templating.WireframeDto;
import com.microel.trackerbackend.storage.entities.templating.Wireframe;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface WireframeRepository extends JpaRepository<Wireframe, Long>, JpaSpecificationExecutor<Wireframe> {
    List<Wireframe> findAllByDeletedIsFalse(Sort sorting);

    Optional<Wireframe> findByWireframeIdAndDeleted(Long id, Boolean isDeleted);

    WireframeDto findByWireframeId(Long id);
}
