package com.microel.trackerbackend.storage.entities.team.util;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "positions")
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionId;
    @Column(length = 48)
    private String name;
    @Column(length = 255)
    private String description;
    private Integer access;
    private Timestamp created;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}
