package com.microel.trackerbackend.storage.repositories;

import com.microel.trackerbackend.storage.entities.acp.NetworkConnectionLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface NetworkConnectionLocationRepository extends JpaRepository<NetworkConnectionLocation, Long>, JpaSpecificationExecutor<NetworkConnectionLocation> {
    NetworkConnectionLocation findFirstByDhcpBindingIdAndIsLast(Integer dhcpBindingId, boolean last);

    List<NetworkConnectionLocation> findAllByCommutatorIdAndPortNameLikeIgnoreCaseAndIsLast(Integer commutatorId, String portName, Boolean isLast);

    List<NetworkConnectionLocation> findAllByCommutatorIdAndIsLast(Integer commutatorId, Boolean isLast);

    List<NetworkConnectionLocation> findAllByDhcpBindingId(Integer id);
}
