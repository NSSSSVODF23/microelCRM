package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FdbItemRepository extends JpaRepository<FdbItem, Long>, JpaSpecificationExecutor<FdbItem> {
    List<FdbItem> findAllByPortInfo_PortInfoId(Long id);
}
