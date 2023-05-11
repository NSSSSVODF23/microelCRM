package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "cities")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cityId;
    @Column(length = 48, unique = true)
    private String name;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    @OneToMany(mappedBy = "city")
    @BatchSize(size = 25)
    @JsonBackReference
    private Set<Street> streets;

    @Override
    public String toString() {
        return "City{" + name + "}";
    }
}
