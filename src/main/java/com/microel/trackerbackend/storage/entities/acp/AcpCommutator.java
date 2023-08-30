package com.microel.trackerbackend.storage.entities.acp;

import com.microel.trackerbackend.services.external.acp.types.Switch;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "acp_commutators")
public class AcpCommutator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long acpCommutatorId;
    private Integer externalId;
    private Boolean available;
    private Timestamp lastUpdate;
    private Boolean deleted;

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
