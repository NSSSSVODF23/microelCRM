package com.microel.trackerbackend.services.external.acp.types;

import com.microel.trackerbackend.storage.entities.address.Address;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Builder
public class SwitchEditingPreset {
    private Switch targetCommutator;
    @Nullable
    private Address address;
    private SwitchModel model;
    private SwitchWithAddress uplinkCommutator;

    public static SwitchEditingPreset from(Switch targetCommutator, @Nullable Address address, SwitchModel model, SwitchWithAddress uplinkCommutator) {
        return SwitchEditingPreset.builder()
                .targetCommutator(targetCommutator)
                .address(address)
                .model(model)
                .uplinkCommutator(uplinkCommutator)
                .build();
    }
}
