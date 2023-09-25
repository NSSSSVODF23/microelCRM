package com.microel.trackerbackend.storage.entities.acp.commutator;

import com.microel.trackerbackend.services.external.acp.types.Switch;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "acp_commutators")
public class AcpCommutator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long acpCommutatorId;
    private Integer externalId;
    private Boolean available;
    private Timestamp lastUpdate;
    private Boolean deleted;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "f_system_info_id")
    private SystemInfo systemInfo;
    @OneToMany(mappedBy = "commutator", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private List<PortInfo> ports;

    public List<PortInfo> getPorts() {
        return ports.stream().sorted(Comparator.comparing(PortInfo::getPortId, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
    }

    public void setPorts(List<PortInfo> ports) {
        this.ports = ports.stream().peek(portInfo -> portInfo.setCommutator(this)).collect(Collectors.toList());
    }

    public static AcpCommutator of(Switch sw) {
        return AcpCommutator.builder()
                .externalId(sw.getId())
                .available(false)
                .lastUpdate(Timestamp.from(Instant.now()))
                .deleted(false)
                .build();
    }

    @Override
    public String toString() {
        String sb = "AcpCommutator{" +
                "acpCommutatorId=" + acpCommutatorId +
                ", externalId=" + externalId +
                ", available=" + available +
                ", lastUpdate=" + lastUpdate +
                ", deleted=" + deleted +
                '}';
        return sb;
    }
}

