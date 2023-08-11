package com.microel.trackerbackend.storage.entities.templating;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "connection_service_items")
public class DataConnectionService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dataConnectionServiceId;
    @Enumerated(EnumType.STRING)
    private ConnectionService connectionService;
}
