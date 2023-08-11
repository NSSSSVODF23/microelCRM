package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.storage.repositories.ClientEquipmentRealizationRepository;
import org.springframework.stereotype.Component;

@Component
public class ClientEquipmentRealizationDispatcher {

    private final ClientEquipmentRealizationRepository clientEquipmentRealizationRepository;

    public ClientEquipmentRealizationDispatcher(ClientEquipmentRealizationRepository clientEquipmentRealizationRepository) {
        this.clientEquipmentRealizationRepository = clientEquipmentRealizationRepository;
    }
}
