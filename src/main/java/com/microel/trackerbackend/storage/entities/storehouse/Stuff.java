package com.microel.trackerbackend.storage.entities.storehouse;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "stuffs")
public class Stuff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stuffId;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_group_items_id")
    private ItemsGroup group;
    private String photo;
    private String name;
    @Column(length = 512)
    private String description;
    private Integer buyPrice;
    private Integer rentPrice;
    private Integer instalmentsPrice;
    private Short instalmentsMonth;
    private Integer purchasePrice;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
}
