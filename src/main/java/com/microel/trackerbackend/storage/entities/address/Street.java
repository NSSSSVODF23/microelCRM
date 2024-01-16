package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microel.trackerbackend.misc.AbstractForm;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "streets")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Street implements Comparable<Street> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long streetId;
    @Column(length = 48)
    private String name;
    @Column(length = 512)
    private String altNames;
    @Column(length = 128)
    @Nullable
    private String billingAlias;
    @Column(length = 24)
    private String prefix;
    @ManyToOne
    @JoinColumn(name = "f_city_id")
    private City city;
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    @OneToMany(mappedBy = "street", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @BatchSize(size = 25)
    private Set<House> houses;

    @JsonIgnore
    public String getPrefixedName() {
        return getPrefix() + " " + getName();
    }

    @JsonIgnore
    public List<Address> getAddress(@Nullable Boolean isAcpConnected) {
        return houses.stream()
                .filter(house -> {
                    if(isAcpConnected != null) {
                        if(isAcpConnected) {
                            return house.getAcpHouseBind() != null && !house.isSomeDeleted();
                        }else{
                            return house.getAcpHouseBind() == null && !house.isSomeDeleted();
                        }
                    }
                    return !house.isSomeDeleted();
                })
                .map(House::getAddress).toList();
    }

    public void setHouses(Set<House> houses) {
        this.houses = houses;
        houses.forEach(house -> house.setStreet(this));
    }

    public String getNameWithPrefix() {
        return prefix + "." + name;
    }

    public String getStreetName() {
        if(this.city == null) return "";
        return city.getName().substring(0,3) + ". " + getNameWithPrefix();
    }

    public Suggestion toSuggestion(){
        return new Suggestion(city.getCityId(), streetId, getStreetName());
    }

    @Override
    public String toString() {
        return "Street{" + name + "}";
    }

    @Override
    @JsonIgnore
    public int compareTo(@NonNull Street o) {
        return Comparator.comparing(Street::getName).compare(this, o);
    }

    @Getter
    @Setter
    public static class Form implements AbstractForm {
        private String prefix;
        private String name;
        @Nullable
        private List<String> altNames;
        @Nullable
        private String billingAlias;

        public boolean isValid() {
            return name != null && !name.isBlank() && prefix != null && !prefix.isBlank();
        }
    }

    @Data
    public static class Suggestion{
        @NonNull
        private Long cityId;
        @NonNull
        private Long streetId;
        @NonNull
        private String name;
    }
}
