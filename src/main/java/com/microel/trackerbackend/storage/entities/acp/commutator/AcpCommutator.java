package com.microel.trackerbackend.storage.entities.acp.commutator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.external.acp.types.Switch;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH,  CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "f_system_info_id")
    private SystemInfo systemInfo;
    @OneToMany(mappedBy = "commutator", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH,  CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 25)
    private List<PortInfo> ports;
    @OneToMany(mappedBy = "commutator", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH,  CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 25)
    private List<RemoteUpdateLog> remoteUpdateLogs;

//    public List<PortInfo> getPorts() {
//        return ports.stream().sorted(Comparator.comparing(PortInfo::getPortId, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
//    }

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

    public void clearPorts() {
        getPorts().removeAll(getPorts().stream().peek(p -> {
            p.setCommutator(null);
        }).toList());
    }

    public void appendPorts(List<PortInfo> ports) {
        for(PortInfo port : ports){
            port.setCommutator(this);
            getPorts().add(port);
        }
    }

    public Boolean getIsLastUpdateError(){
        if(getRemoteUpdateLogs() == null || getRemoteUpdateLogs().isEmpty()) return false;
        return getRemoteUpdateLogs().get(getRemoteUpdateLogs().size() - 1).getIsError();
    }

    public String getLastErrorMessage() {
        if (getRemoteUpdateLogs() == null || getRemoteUpdateLogs().isEmpty()) return "";
        RemoteUpdateLog lastLog = getRemoteUpdateLogs().get(getRemoteUpdateLogs().size() - 1);
        if (lastLog.getIsError()) return lastLog.getMessage();
        return "";
    }

    public void appendRemoteUpdateLog(RemoteUpdateLog remoteUpdateLog) {
        remoteUpdateLog.setCommutator(this);
        getRemoteUpdateLogs().add(remoteUpdateLog);
    }

    public void removeOldRemoteUpdateLogs() {
        if(getRemoteUpdateLogs().size() > 19){
            getRemoteUpdateLogs().remove(0);
        }
    }

    @JsonIgnore
    public Integer getMacTableSize() {
        return getPorts().stream().map(p -> {
            if(p == null || p.getMacTable() == null) return 0;
            return p.getMacTable().size();
        }).reduce(Integer::sum).orElse(0);
    }
}

