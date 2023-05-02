package com.microel.trackerbackend.storage.entities.storehouse;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "items_groups")
public class ItemsGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemsGroupId;
    @Column(length = 48)
    private String name;
    private String description;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "f_parent_items_group_id")
    private ItemsGroup parent;
}
