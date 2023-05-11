package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "houses")
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long houseId;
    private Short houseNum;
    private Character letter;
    private Short fraction;
    private Short build;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JsonManagedReference
    private Street street;

    @JsonIgnore
    public Address getAddress() {
        Address address = new Address();
        address.setCity(street.getCity());
        address.setStreet(street);
        address.setHouse(this);
        return address;
    }

    @JsonIgnore
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (street != null) {
            if (street.getPrefix() != null) {
                sb.append(street.getPrefix()).append(".");
            }
            if (street.getName() != null) {
                sb.append(street.getName());
            }
        }
        if (houseNum != null)
            sb.append(" ").append(houseNum);

        if (fraction != null) {
            sb.append("/").append(fraction);
        }
        if (letter != null) {
            sb.append(letter);
        }
        if (build != null) {
            sb.append(" стр.").append(build);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "House{" + "houseId=" + houseId +
                ", houseNum=" + houseNum +
                ", letter=" + letter +
                ", fraction=" + fraction +
                ", build=" + build +
                ", street=" + street +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof House)) return false;
        House house = (House) o;
        return Objects.equals(getHouseId(), house.getHouseId()) && Objects.equals(getHouseNum(), house.getHouseNum()) && Objects.equals(getLetter(), house.getLetter()) && Objects.equals(getFraction(), house.getFraction()) && Objects.equals(getBuild(), house.getBuild());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHouseId(), getHouseNum(), getLetter(), getFraction(), getBuild());
    }
}
