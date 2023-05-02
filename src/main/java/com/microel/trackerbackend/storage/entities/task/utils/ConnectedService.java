package com.microel.trackerbackend.storage.entities.task.utils;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "connected_services")
public class ConnectedService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long connectedServiceId;
    private String name;
    private ConnectingType connectingType;
    private Float price;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}
