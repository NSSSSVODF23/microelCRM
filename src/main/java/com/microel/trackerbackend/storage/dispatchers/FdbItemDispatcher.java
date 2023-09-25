package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.repositories.FdbItemRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FdbItemDispatcher {
    private final FdbItemRepository fdbItemRepository;

    public FdbItemDispatcher(FdbItemRepository fdbItemRepository) {
        this.fdbItemRepository = fdbItemRepository;
    }

    public void remove(FdbItem fdbItem) {
        fdbItemRepository.delete(fdbItem);
    }

    public void removeAll(List<Long> fdbItems) {
        fdbItemRepository.deleteAllById(fdbItems.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public List<FdbItem> getFdbByPort(Long id) {
        return fdbItemRepository.findAllByPortInfo_PortInfoId(id);
    }
}
