package com.microel.trackerbackend.storage.entities.acp.commutator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "acp_comm_ports_info")
public class PortInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portInfoId;
    private Status status;
    private String name;
    @Nullable
    private Speed speed;
    private Boolean force = false;
    private InterfaceType type = InterfaceType.ETHERNET;
    private PortType portType = PortType.COPPER;
    @Nullable
    private Integer uptime;
    @Nullable
    private String description;
    @JsonIgnore
    @ManyToOne
    private AcpCommutator commutator;
    @JsonIgnore
    @OneToMany(mappedBy = "portInfo")
    private List<FdbItem> macTable = new ArrayList<>();

    @Nullable
    public Integer getPortId() {
        try {
            return Integer.parseInt(name);
        }catch (NumberFormatException e){
            return null;
        }
    }

    public enum Status {
        UP("UP"), DOWN("DOWN"), ADMIN_DOWN("ADMIN_DOWN"), PREPARE("PREPARE");

        private String status;

        Status(String status) {
            this.status = status;
        }
    }

    public enum Speed {
        HALF10("HALF10"), FULL10("FULL10"), HALF100("HALF100"), FULL100("FULL100"), HALF1000("HALF1000"), FULL1000("FULL1000");

        private String speed;

        Speed(String speed) {
            this.speed = speed;
        }
    }

    public enum InterfaceType{
        ETHERNET("ETHERNET"), GIGABIT("GIGABIT"), TENGIGABIT("TENGIGABIT");

        private String type;

        InterfaceType(String type){
            this.type = type;
        }
    }

    public enum PortType{
        COPPER("COPPER"), FIBER("FIBER"), PON("PON");

        private String type;

        PortType(String type){
            this.type = type;
        }
    }
}
