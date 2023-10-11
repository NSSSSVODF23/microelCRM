package com.microel.trackerbackend.storage.entities.acp.commutator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "remote_update_logs")
public class RemoteUpdateLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long remoteUpdateLogId;
    private Boolean isError;
    @Column(columnDefinition = "text default ''")
    private String message;
    private Timestamp timestamp;
    @ManyToOne
    @JsonIgnore
    private AcpCommutator commutator;

    public static RemoteUpdateLog error(Throwable e) {
        return RemoteUpdateLog.builder()
                .isError(true)
                .message(e.getMessage())
                .timestamp(Timestamp.from(Instant.now()))
                .build();
    }

    public static RemoteUpdateLog success(Integer portsCount, Integer macsCount) {
        return RemoteUpdateLog.builder()
                .isError(false)
                .message("Обновление успешно завершено. Получено портов: " + portsCount + " шт., mac-адресов: " + macsCount + " шт.")
                .timestamp(Timestamp.from(Instant.now()))
                .build();
    }
}
