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
import java.util.List;

@Component
public class NetworkConnectionLocationDispatcher {
    private final NetworkConnectionLocationRepository networkConnectionLocationRepository;

    public NetworkConnectionLocationDispatcher(NetworkConnectionLocationRepository networkConnectionLocationRepository) {
        this.networkConnectionLocationRepository = networkConnectionLocationRepository;
    }

    public NetworkConnectionLocation checkAndWrite(DhcpBinding existingSession, Switch commutator, PortInfo port, FdbItem fdbItem) {
        NetworkConnectionLocation lastConnectionLocation = networkConnectionLocationRepository.findFirstByDhcpBindingIdAndIsLast(existingSession.getId(), true);
        if (lastConnectionLocation == null) {
            NetworkConnectionLocation newLocation = NetworkConnectionLocation.of(existingSession, commutator, port, fdbItem);
            return networkConnectionLocationRepository.save(newLocation);
        }else{
            if(lastConnectionLocation.isLocationRelevant(commutator, port, fdbItem)){
                lastConnectionLocation.setCommutatorName(commutator.getName());
                lastConnectionLocation.setVlanName(fdbItem.getVlanName());
                lastConnectionLocation.setPortId(port.getPortInfoId());
                lastConnectionLocation.setCheckedAt(Timestamp.from(Instant.now()));
                return networkConnectionLocationRepository.save(lastConnectionLocation);
            }else{
                lastConnectionLocation.setIsLast(false);
                networkConnectionLocationRepository.save(lastConnectionLocation);
                NetworkConnectionLocation newLocation = NetworkConnectionLocation.of(existingSession, commutator, port, fdbItem);
                return networkConnectionLocationRepository.save(newLocation);
            }
        }
    }

    @Nullable
    public NetworkConnectionLocation getByBindingId(Integer bindingId) {
        return networkConnectionLocationRepository.findFirstByDhcpBindingIdAndIsLast(bindingId, true);
    }

    public List<NetworkConnectionLocation> getLastByCommutator(Integer id, @Nullable Integer port) {
        if( port == null) {
            return networkConnectionLocationRepository.findAllByCommutatorIdAndIsLast(id, true);
        }
        return networkConnectionLocationRepository.findAllByCommutatorIdAndPortNameLikeIgnoreCaseAndIsLast(id, "%"+port+"%", true);
    }

    public List<NetworkConnectionLocation> getAllByBindingId(Integer id) {
        return networkConnectionLocationRepository.findAllByDhcpBindingId(id);
    }
}
