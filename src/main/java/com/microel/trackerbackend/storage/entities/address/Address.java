package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Comparator;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "addresses")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address implements Comparable<Address> {
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

    public String getAddressName() {
        StringBuilder addressName = new StringBuilder();
        if(city != null) {
            addressName.append(city.getName(), 0, 4).append(".");
        }
        if(street  != null) {
            addressName.append(" ");
            if(street.getPrefix() != null){
                addressName.append(street.getPrefix()).append(".");
            }
            if(street.getName() != null){
                addressName.append(street.getName());
            }
        }
        if(houseNum != null) {
            addressName.append(" ").append(houseNum);
        }
        if(fraction != null) {
            addressName.append("/").append(fraction);
        }
        if(letter != null) {
            addressName.append(letter);
        }
        if(build != null) {
            addressName.append(" стр.").append(build);
        }
        if(apartmentNum != null) {
            addressName.append(" кв.").append(apartmentNum);
        }
        if(entrance != null) {
            addressName.append(" под.").append(entrance);
        }
        if(floor != null) {
            addressName.append(" эт.").append(floor);
        }
        if(apartmentMod != null) {
            addressName.append(" (").append(apartmentMod).append(")");
        }
        return addressName.toString();
    }

    public void setAddressName(String addressName) {
    }

    public void setHouse(House house){
        this.houseNum = house.getHouseNum();
        this.fraction = house.getFraction();
        this.letter = house.getLetter();
        this.build = house.getBuild();
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

    @Override
    @JsonIgnore
    public int compareTo(@NonNull Address o) {
        return Comparator.comparing(Address::getCity,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getStreet,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getHouseNum,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getFraction,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getLetter,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getBuild,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getEntrance,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getFloor,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getApartmentNum,Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Address::getApartmentMod,Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(this, o);
    }
}
