package com.microel.trackerbackend.services.external.acp.types;

import com.microel.trackerbackend.misc.AbstractForm;
import com.microel.trackerbackend.storage.entities.acp.commutator.AcpCommutator;
import com.microel.trackerbackend.storage.entities.address.Address;
import lombok.*;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.lang.Nullable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Switch {
    private Integer id;
    private String name;
    private Short swtype;
    private Short swmodelId;
    private String ipaddr;
    private Short protocol;
    private String login;
    private String passw;
    private Integer buildId;
    private Short entrance;
    private Short storey;
    private Integer phyUplinkId;
    private String description;
    private Short status;
    private Short enableMonitor;
    private Short enableSms;
    private Short enableBackup;
    @Nullable
    private Address address;
    @Nullable
    private AcpCommutator additionalInfo;

    @Getter
    @Setter
    public static class Form implements AbstractForm {
        private Short swtype;
        private Short swmodelId;
        private String name;
        private String ipaddr;
        private Integer buildId;
        private Short entrance;
        private Short storey;
        private Integer phyUplinkId;
        private Short enableMonitor;
        private Short enableSms;
        @Override
        public boolean isValid() {
            if(swtype == null) return false;
            if(swmodelId == null) return false;
            if(name == null || name.isBlank()) return false;
            if(ipaddr == null || ipaddr.isBlank() || !InetAddressValidator.getInstance().isValid(ipaddr)) return false;
            if(buildId == null) return false;
            if(entrance == null) return false;
            if(storey == null) return false;
            if(phyUplinkId == null) return false;
            return true;
        }
    }
}
