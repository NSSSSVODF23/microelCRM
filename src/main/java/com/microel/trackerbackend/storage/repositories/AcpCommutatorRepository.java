package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.acp.AcpCommutator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AcpCommutatorRepository extends JpaRepository<AcpCommutator, Long>, JpaSpecificationExecutor<AcpCommutator> {
    List<AcpCommutator> findAllByAcpCommutatorIdIn(List<Long> removedCommutators);

    AcpCommutator findTopByExternalId(Integer id);
}
