package com.microel.trackerbackend.storage.entities.acp.commutator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "acp_comm_fdb_items")
public class FdbItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fdbItemId;
    private Integer vid;
    private String vlanName;
    private String mac;
    private Integer portId;
    private Boolean dynamic;
    @ManyToOne
    @JsonIgnore
    private PortInfo portInfo;
    @Transient
    @Nullable
    private DhcpBinding dhcpBinding;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FdbItem fdbItem)) return false;
        return Objects.equals(getVid(), fdbItem.getVid()) && Objects.equals(getVlanName(), fdbItem.getVlanName()) && Objects.equals(getMac(), fdbItem.getMac()) && Objects.equals(getPortId(), fdbItem.getPortId()) && Objects.equals(getDynamic(), fdbItem.getDynamic());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVid(), getVlanName(), getMac(), getPortId(), getDynamic());
    }
}
