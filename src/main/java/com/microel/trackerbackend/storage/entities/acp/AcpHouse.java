package com.microel.trackerbackend.storage.entities.acp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.*;

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

    public String getFullName(){
        return streetName + " " + houseNum;
    }
}
