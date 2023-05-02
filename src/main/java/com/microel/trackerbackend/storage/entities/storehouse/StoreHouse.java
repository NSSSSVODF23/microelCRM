package com.microel.trackerbackend.storage.entities.storehouse;

import antlr.CommonAST;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "store_house")
public class StoreHouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long storeHouseId;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_stuff_id")
    private Stuff stuff;
    private Integer reserved;
    private Integer count;
}
