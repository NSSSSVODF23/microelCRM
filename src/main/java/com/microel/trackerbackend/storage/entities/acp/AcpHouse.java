package com.microel.trackerbackend.storage.entities.acp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microel.trackerbackend.services.external.acp.types.Network;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "acp_houses")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AcpHouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long acpHouseId;

    private Integer streetId;
    private String streetName;
    private Integer buildingId;
    private String houseNum;
    @Nullable
    @Transient
    private AcpHouse uplink;
    @Nullable
    @Transient
    private List<AcpHouse> downlinks;
    @Nullable
    @Transient
    private List<Network> networks;

    public String getFullName(){
        return streetName + " " + houseNum;
    }
}
