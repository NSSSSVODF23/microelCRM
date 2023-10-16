package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.storage.entities.acp.NetworkConnectionLocation;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.repositories.NetworkConnectionLocationRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class NetworkConnectionLocationDispatcher {
    private final NetworkConnectionLocationRepository networkConnectionLocationRepository;
    private final StompController stompController;

    public NetworkConnectionLocationDispatcher(NetworkConnectionLocationRepository networkConnectionLocationRepository, StompController stompController) {
        this.networkConnectionLocationRepository = networkConnectionLocationRepository;
        this.stompController = stompController;
    }

    public NetworkConnectionLocation checkAndWrite(DhcpBinding existingSession, Switch commutator, PortInfo port, FdbItem fdbItem) {
        NetworkConnectionLocation lastConnectionLocation = networkConnectionLocationRepository.findFirstByDhcpBindingIdAndIsLast(existingSession.getId(), true);
        if (lastConnectionLocation == null) {
            NetworkConnectionLocation newLocation = NetworkConnectionLocation.of(existingSession, commutator, port, fdbItem);
            NetworkConnectionLocation location = networkConnectionLocationRepository.save(newLocation);
            return location;

//            if(commutator.getAdditionalInfo() != null && commutator.getAdditionalInfo().getLastUpdate() != null && commutator.getAdditionalInfo().getPorts() != null) {
//                location.setCommutatorInfo(commutator.getAdditionalInfo().getLastUpdate(), commutator.getAdditionalInfo().getPorts());
//                existingSession.setLastConnectionLocation(location);
//                stompController.updateDhcpBinding(existingSession);
//            }
        }else{
            if(lastConnectionLocation.isLocationRelevant(commutator, port, fdbItem)){
                lastConnectionLocation.setCommutatorName(commutator.getName());
                lastConnectionLocation.setVlanName(fdbItem.getVlanName());
                lastConnectionLocation.setPortId(port.getPortInfoId());
                lastConnectionLocation.setCheckedAt(Timestamp.from(Instant.now()));
                NetworkConnectionLocation location = networkConnectionLocationRepository.save(lastConnectionLocation);
                return location;

//                if(commutator.getAdditionalInfo() != null && commutator.getAdditionalInfo().getLastUpdate() != null && commutator.getAdditionalInfo().getPorts() != null) {
//                    location.setCommutatorInfo(commutator.getAdditionalInfo().getLastUpdate(), commutator.getAdditionalInfo().getPorts());
//                    existingSession.setLastConnectionLocation(location);
//                    stompController.updateDhcpBinding(existingSession);
//                }
            }else{
                lastConnectionLocation.setIsLast(false);
                networkConnectionLocationRepository.save(lastConnectionLocation);
                NetworkConnectionLocation newLocation = NetworkConnectionLocation.of(existingSession, commutator, port, fdbItem);
                NetworkConnectionLocation location = networkConnectionLocationRepository.save(newLocation);
                return location;

//                if(commutator.getAdditionalInfo() != null && commutator.getAdditionalInfo().getLastUpdate() != null && commutator.getAdditionalInfo().getPorts() != null) {
//                    location.setCommutatorInfo(commutator.getAdditionalInfo().getLastUpdate(), commutator.getAdditionalInfo().getPorts());
//                    existingSession.setLastConnectionLocation(location);
//                    stompController.updateDhcpBinding(existingSession);
//                }
            }
        }
    }

    @Nullable
    public NetworkConnectionLocation getByBindingId(Integer bindingId) {
        return networkConnectionLocationRepository.findFirstByDhcpBindingIdAndIsLast(bindingId, true);
    }
}
