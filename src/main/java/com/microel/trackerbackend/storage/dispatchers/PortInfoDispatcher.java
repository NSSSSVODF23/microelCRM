package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.repositories.PortInfoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PortInfoDispatcher {
    private final PortInfoRepository portInfoRepository;

    public PortInfoDispatcher(PortInfoRepository portInfoRepository) {
        this.portInfoRepository = portInfoRepository;
    }

    public void remove(PortInfo portInfo){
        portInfoRepository.delete(portInfo);
    }

    public void removeAll(List<Long> portsInfo){
        portInfoRepository.deleteAllById(portsInfo.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public Optional<PortInfo> getById(Long portId) {
        return portInfoRepository.findById(portId);
    }
}
