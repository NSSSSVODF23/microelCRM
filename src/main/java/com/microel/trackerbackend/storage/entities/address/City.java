package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.misc.AbstractForm;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.Comparator;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "cities")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class City implements Comparable<City> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cityId;
    @Column(length = 48, unique = true)
    private String name;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    @OneToMany(mappedBy = "city")
    @BatchSize(size = 25)
    @JsonIgnore
    private Set<Street> streets;

    @Override
    public String toString() {
        return "City{" + name + "}";
    }

    @Override
    @JsonIgnore
    public int compareTo(@NonNull City o) {
        return Comparator.comparing(City::getName).compare(this, o);
    }

    @Getter
    @Setter
    public static class Form implements AbstractForm {
        private String name;

        public boolean isValid() {
            return name != null && !name.isBlank();
        }
    }
}
