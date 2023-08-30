package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.storage.entities.acp.AcpCommutator;
import com.microel.trackerbackend.storage.repositories.AcpCommutatorRepository;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class AcpCommutatorDispatcher {
    private final AcpCommutatorRepository acpCommutatorRepository;

    public AcpCommutatorDispatcher(AcpCommutatorRepository acpCommutatorRepository) {
        this.acpCommutatorRepository = acpCommutatorRepository;
    }

    public void synchronize(List<Switch> allCommutators) {
        List<AcpCommutator> allAcpCommutators = acpCommutatorRepository.findAll();
        List<Integer> allAcpCommutatorIds = allAcpCommutators.stream().map(AcpCommutator::getExternalId).toList();
        List<Integer> allCommutatorIds = allCommutators.stream().map(Switch::getId).toList();
        List<AcpCommutator> newCommutators = allCommutators.stream().filter(acpCommutator -> !allAcpCommutatorIds.contains(acpCommutator.getId())).map(AcpCommutator::of).toList();
        List<AcpCommutator> restoredCommutators = allAcpCommutators.stream()
                .filter(AcpCommutator::getDeleted)
                .filter(acpCommutator -> allCommutatorIds.contains(acpCommutator.getExternalId()))
                .peek(acpCommutator -> {
                    acpCommutator.setDeleted(false);
                    acpCommutator.setLastUpdate(Timestamp.from(Instant.now()));
                }).toList();
        List<AcpCommutator> removedCommutators = allAcpCommutators.stream()
                .filter(acpCommutatorId -> !allCommutatorIds.contains(acpCommutatorId.getExternalId()))
                .peek(acpCommutator -> {
                    acpCommutator.setDeleted(true);
                    acpCommutator.setLastUpdate(Timestamp.from(Instant.now()));
                }).toList();

        acpCommutatorRepository.saveAll(newCommutators);
        acpCommutatorRepository.saveAll(restoredCommutators);
        acpCommutatorRepository.saveAll(removedCommutators);
    }

    public AcpCommutator updateStatus(Integer id, boolean reachable) {
        AcpCommutator commutator = acpCommutatorRepository.findTopByExternalId(id);
        if(commutator != null){
            if(commutator.getAvailable() != reachable){
                commutator.setAvailable(reachable);
                commutator.setLastUpdate(Timestamp.from(Instant.now()));
                return acpCommutatorRepository.save(commutator);
            }
        }
        return null;
    }

    public AcpCommutator getById(Integer id) {
        return acpCommutatorRepository.findTopByExternalId(id);
    }
}
