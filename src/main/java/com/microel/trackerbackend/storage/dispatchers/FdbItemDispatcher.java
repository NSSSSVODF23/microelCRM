package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.repositories.FdbItemRepository;
import com.microel.trackerbackend.storage.repositories.PortInfoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FdbItemDispatcher {
    private final FdbItemRepository fdbItemRepository;
    private final PortInfoRepository portInfoRepository;

    public FdbItemDispatcher(FdbItemRepository fdbItemRepository, PortInfoRepository portInfoRepository) {
        this.fdbItemRepository = fdbItemRepository;
        this.portInfoRepository = portInfoRepository;
    }

    public void remove(FdbItem fdbItem) {
        fdbItemRepository.delete(fdbItem);
    }

    public void removeAll(List<FdbItem> fdbItems) {
        fdbItemRepository.deleteAll(fdbItemRepository.saveAll(fdbItems.stream().peek(item-> {
            PortInfo portInfo = item.getPortInfo();
            portInfo.getMacTable().remove(item);
            portInfoRepository.save(portInfo);
            item.setPortInfo(null);
        }).collect(Collectors.toList())));
    }

    public List<FdbItem> getFdbByPort(Long id) {
        return fdbItemRepository.findAllByPortInfo_PortInfoId(id);
    }


}
