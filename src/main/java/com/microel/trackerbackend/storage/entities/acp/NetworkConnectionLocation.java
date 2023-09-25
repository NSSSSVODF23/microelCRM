package com.microel.trackerbackend.storage.entities.acp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.services.external.acp.types.SwitchWithAddress;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "network_connection_locations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkConnectionLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long networkConnectionLocationId;
    private String commutatorName;
    private String commutatorIp;
    private Integer commutatorId;
    private String portName;
    private Long portId;
    private Integer vid;
    private String vlanName;
    private Integer dhcpBindingId;
    private Boolean isLast;
    private Timestamp createdAt;
    private Timestamp checkedAt;

    @JsonIgnore
    public Boolean isLocationRelevant(Switch targetCommutator, PortInfo targetPort, FdbItem fdbItem){
        if(!Objects.equals(targetCommutator.getId(), commutatorId))
            return false;
        if(!targetCommutator.getIpaddr().equals(commutatorIp))
            return false;
        if(!Objects.equals(targetPort.getPortInfoId(), portId))
            return false;
        if(!targetPort.getName().equals(portName))
            return false;
        if(!Objects.equals(fdbItem.getVid(), vid))
            return false;

        return true;
    }

    public static NetworkConnectionLocation of(DhcpBinding session, Switch sw, PortInfo port, FdbItem fdbItem){
        Timestamp createAndChecked = Timestamp.from(Instant.now());
        return NetworkConnectionLocation.builder()
                .commutatorName(sw.getName())
                .commutatorIp(sw.getIpaddr())
                .commutatorId(sw.getId())
                .portName(port.getName())
                .portId(port.getPortInfoId())
                .vid(fdbItem.getVid())
                .vlanName(fdbItem.getVlanName())
                .dhcpBindingId(session.getId())
                .isLast(true)
                .createdAt(createAndChecked)
                .checkedAt(createAndChecked)
                .build();
    }
}
