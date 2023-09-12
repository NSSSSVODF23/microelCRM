package com.microel.trackerbackend.storage.entities.acp.commutator;

import lombok.*;

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
    private PortInfo portInfo;
}
