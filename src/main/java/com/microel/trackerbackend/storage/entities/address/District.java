package com.microel.trackerbackend.storage.entities.address;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "districts")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class District {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long districtId;
    @Column(length = 48, unique = true)
    private String name;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;

    @Override
    public String toString() {
        return "District{" + name + "}";
    }
}
