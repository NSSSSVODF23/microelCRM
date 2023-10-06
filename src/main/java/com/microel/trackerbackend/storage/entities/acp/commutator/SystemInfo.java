package com.microel.trackerbackend.storage.entities.acp.commutator;

import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "acp_comm_systems_info")
public class SystemInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long systemInfoId;
    private String device;
    @Nullable
    private String mac;
    private String hwVersion;
    private String fwVersion;
    @Nullable
    private Integer uptime;
    private Timestamp lastUpdate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemInfo that)) return false;
        return Objects.equals(getDevice(), that.getDevice()) && Objects.equals(getMac(), that.getMac()) && Objects.equals(getHwVersion(), that.getHwVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDevice(), getMac(), getHwVersion());
    }
}
