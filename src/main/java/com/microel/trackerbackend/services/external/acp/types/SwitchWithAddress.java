package com.microel.trackerbackend.services.external.acp.types;

import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
public class SwitchWithAddress {
    private Switch commutator;
    private AcpHouse address;

    @Nullable
    public String getLabel() {
        if(commutator == null || address == null) return null;
        return commutator.getName() + " | " + address.getFullName();
    }

    @Nullable
    public Integer getValue(){
        if(commutator == null) return null;
        return commutator.getId();
    }
}
