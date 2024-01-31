package com.microel.trackerbackend.storage.entities.task;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "types_of_contracts")
public class TypesOfContracts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long typeOfContractId;

}
