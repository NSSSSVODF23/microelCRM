package com.microel.trackerbackend.storage.entities.address;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "addresses")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_city_id")
    private City city;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_district_id")
    private District district;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_street_id")
    private Street street;

    private Short houseNum;
    private Short fraction;
    private Character letter;
    private Short build;
    private Short entrance;
    private Short floor;

    private Short apartmentNum;
    @Column(length = 32)
    private String apartmentMod;

    public void setCityByName(String cityName) {
        this.city = City.builder().name(cityName).deleted(false).build();
    }

    @Override
    public String toString() {
        return "Address{" +
                "addressId=" + addressId +
                ", city=" + city +
                ", district=" + district +
                ", street=" + street +
                ", houseNum=" + houseNum +
                ", fraction=" + fraction +
                ", letter=" + letter +
                ", build=" + build +
                ", entrance=" + entrance +
                ", floor=" + floor +
                ", apartmentNum=" + apartmentNum +
                ", apartmentMod='" + apartmentMod +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address address = (Address) o;
        return Objects.equals(getCity(), address.getCity()) && Objects.equals(getDistrict(), address.getDistrict())
                && Objects.equals(getStreet(), address.getStreet()) && Objects.equals(getHouseNum(), address.getHouseNum())
                && Objects.equals(getFraction(), address.getFraction()) && Objects.equals(getLetter(), address.getLetter())
                && Objects.equals(getBuild(), address.getBuild()) && Objects.equals(getEntrance(), address.getEntrance())
                && Objects.equals(getFloor(), address.getFloor()) && Objects.equals(getApartmentNum(), address.getApartmentNum())
                && Objects.equals(getApartmentMod(), address.getApartmentMod());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCity(), getDistrict(), getStreet(), getHouseNum(), getFraction(), getLetter(), getBuild(), getEntrance(), getFloor(), getApartmentNum(), getApartmentMod());
    }
}
