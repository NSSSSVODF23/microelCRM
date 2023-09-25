package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PortInfoRepository extends JpaRepository<PortInfo, Long>, JpaSpecificationExecutor<PortInfo> {
}
