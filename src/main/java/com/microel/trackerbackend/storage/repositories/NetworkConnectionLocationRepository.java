package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.acp.NetworkConnectionLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NetworkConnectionLocationRepository extends JpaRepository<NetworkConnectionLocation, Long>, JpaSpecificationExecutor<NetworkConnectionLocation> {
    NetworkConnectionLocation findFirstByDhcpBindingIdAndIsLast(Integer dhcpBindingId, boolean last);
}
