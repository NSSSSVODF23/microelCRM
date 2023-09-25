package com.microel.trackerbackend.storage.entities.acp.commutator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.services.external.acp.types.DhcpBinding;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import javax.persistence.*;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private PortInfo portInfo;
    @Transient
    @Nullable
    private DhcpBinding dhcpBinding;
}
