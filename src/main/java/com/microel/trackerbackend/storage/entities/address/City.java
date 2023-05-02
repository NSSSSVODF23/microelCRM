package com.microel.trackerbackend.storage.entities.address;

import lombok.*;

import javax.persistence.*;

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

    @Override
    public String toString() {
        return "City{" + name + "}";
    }
}
