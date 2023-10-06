package com.microel.trackerbackend.storage.entities.acp.commutator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    @OneToMany(mappedBy = "portInfo", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 25)
    private List<FdbItem> macTable = new ArrayList<>();
    @JsonIgnore
    private Boolean forceDownlink = false;

    public Boolean isDownlink(){
        return (forceDownlink != null && forceDownlink) || macTable.stream().anyMatch(fdbItem -> fdbItem.getVid() == 100 || fdbItem.getVid() == 90 || fdbItem.getVid() == 101 || fdbItem.getVid() == 110);
    }

    public void appendToMacTable(FdbItem fdbItem){
        fdbItem.setPortInfo(this);
        this.macTable.add(fdbItem);
    }

    public void setMacTable(List<FdbItem> macTable) {
        this.macTable = macTable.stream().peek(fdbItem -> fdbItem.setPortInfo(this)).collect(Collectors.toList());
    }

    @Nullable
    public Integer getPortId() {
        try {
            return Integer.parseInt(name.replaceAll("[^\\d]",""));
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
