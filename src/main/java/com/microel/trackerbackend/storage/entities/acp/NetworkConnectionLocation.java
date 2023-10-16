package com.microel.trackerbackend.storage.entities.acp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
    private Long portId;
    private String portName;
    private Integer vid;
    private String vlanName;
    private Integer dhcpBindingId;
    private Boolean isLast;
    private Timestamp createdAt;
    private Timestamp checkedAt;
    @Transient
    private Boolean isHasLink = false;
    @Nullable
    @Transient
    private PortInfo.Speed portSpeed;
    @Nullable
    @Transient
    private Timestamp lastPortCheck;

    public static NetworkConnectionLocation of(DhcpBinding session, Switch sw, PortInfo port, FdbItem fdbItem) {
        Timestamp createAndChecked = Timestamp.from(Instant.now());
        return NetworkConnectionLocation.builder()
                .commutatorName(sw.getName())
                .commutatorIp(sw.getIpaddr())
                .commutatorId(sw.getId())
                .portId(port.getPortInfoId())
                .portName(port.getName())
                .vid(fdbItem.getVid())
                .vlanName(fdbItem.getVlanName())
                .dhcpBindingId(session.getId())
                .isLast(true)
                .createdAt(createAndChecked)
                .checkedAt(createAndChecked)
                .build();
    }

    @JsonIgnore
    public Boolean isLocationRelevant(Switch targetCommutator, PortInfo targetPort, FdbItem fdbItem) {
        return Objects.equals(targetCommutator.getId(), commutatorId)
                && Objects.equals(targetCommutator.getIpaddr(), commutatorIp)
                && Objects.equals(targetPort.getName(), portName)
                && Objects.equals(fdbItem.getVid(), vid);
    }

    public void setCommutatorInfo(Timestamp lastUpdate, List<PortInfo> ports) {
        setLastPortCheck(lastUpdate);
        if (getPortId() != null) {
            ports.stream().filter(port -> port.getPortInfoId().equals(getPortId())).findFirst().ifPresent(portInfo -> {
                setIsHasLink(Objects.equals(portInfo.getStatus(), PortInfo.Status.UP));
                setPortSpeed(portInfo.getSpeed());
            });
        }
    }
}
