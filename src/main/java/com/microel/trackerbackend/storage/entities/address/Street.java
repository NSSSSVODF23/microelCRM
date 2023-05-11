package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.Set;

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
    @Column(length = 512)
    @JsonIgnore
    private String altNames;
    @Column(length = 24)
    private String prefix;
    @ManyToOne
    @JoinColumn(name = "f_city_id")
    @JsonManagedReference
    private City city;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    @OneToMany(mappedBy = "street", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonBackReference
    @BatchSize(size = 25)
    private Set<House> houses;

    public void setHouses(Set<House> houses) {
        this.houses = houses;
        houses.forEach(house -> house.setStreet(this));
    }

    @Override
    public String toString() {
        return "Street{" + name + "}";
    }
}
