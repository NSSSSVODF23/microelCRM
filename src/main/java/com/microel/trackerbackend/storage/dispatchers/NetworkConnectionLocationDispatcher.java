package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.services.external.acp.types.SwitchWithAddress;
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

    public NetworkConnectionLocationDispatcher(NetworkConnectionLocationRepository networkConnectionLocationRepository) {
        this.networkConnectionLocationRepository = networkConnectionLocationRepository;
    }

    public void checkAndWrite(DhcpBinding existingSession, Switch commutator, PortInfo port, FdbItem fdbItem) {
        NetworkConnectionLocation lastConnectionLocation = networkConnectionLocationRepository.findFirstByDhcpBindingIdAndIsLast(existingSession.getId(), true);
        if (lastConnectionLocation == null) {
            NetworkConnectionLocation newLocation = NetworkConnectionLocation.of(existingSession, commutator, port, fdbItem);
            networkConnectionLocationRepository.save(newLocation);
        }else{
            if(lastConnectionLocation.isLocationRelevant(commutator, port, fdbItem)){
                lastConnectionLocation.setCommutatorName(commutator.getName());
                lastConnectionLocation.setVlanName(fdbItem.getVlanName());
                lastConnectionLocation.setCheckedAt(Timestamp.from(Instant.now()));
                networkConnectionLocationRepository.save(lastConnectionLocation);
            }else{
                lastConnectionLocation.setIsLast(false);
                networkConnectionLocationRepository.save(lastConnectionLocation);
                NetworkConnectionLocation newLocation = NetworkConnectionLocation.of(existingSession, commutator, port, fdbItem);
                networkConnectionLocationRepository.save(newLocation);
            }
        }
    }

    @Nullable
    public NetworkConnectionLocation getByBindingId(Integer bindingId) {
        return networkConnectionLocationRepository.findFirstByDhcpBindingIdAndIsLast(bindingId, true);
    }
}
