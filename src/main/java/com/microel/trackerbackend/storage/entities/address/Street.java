package com.microel.trackerbackend.storage.entities.address;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "streets")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Street {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long streetId;
    @Column(length = 48)
    private String name;
    @ManyToOne
    @JoinColumn(name = "f_city_id")
    private City city;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;

    @Override
    public String toString() {
        return "Street{" + name + "}";
    }
}
