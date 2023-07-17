package com.microel.trackerbackend.storage.entities.salary;

import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "actions_taken")
public class ActionTaken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionTakenId;
    private String workName;
    @OneToOne
    private PaidAction paidAction;
    private Integer count;
    private UUID uuid;
}
