package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import javax.lang.model.type.ReferenceType;
import javax.persistence.*;
import java.util.Comparator;
import java.util.Map;
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

    @Nullable
    private Long houseId;

    private Short houseNum;
    private Short fraction;
    private Character letter;
    private Short build;
    private Short entrance;
    private Short floor;

    private Short apartmentNum;
    @Column(length = 32)
    private String apartmentMod;

    @Nullable
    @ManyToOne
    private AcpHouse acpHouseBind;

    public Integer countOfFilled() {
        Integer count = 0;
        if (street != null) {
            count++;
        }
        if (city != null) {
            count++;
        }
        if (district != null) {
            count++;
        }
        if (street != null) {
            count++;
        }
        if(houseNum != null) {
            count++;
        }
        if(fraction != null) {
            count++;
        }
        if(letter != null) {
            count++;
        }
        if(build != null) {
            count++;
        }
        if(apartmentNum != null) {
            count++;
        }
        if(entrance != null) {
            count++;
        }
        if(floor != null) {
            count++;
        }
        if(apartmentMod != null) {
            count++;
        }
        return count;
    }

    public String getBillingAddress(Boolean withFraction){
        String billingStreetName = getStreet().getBillingAlias();
        if(billingStreetName == null) billingStreetName = getStreet().getName();
        StringBuilder billingAddress = new StringBuilder(billingStreetName);
        if(getHouseNum() != null) billingAddress.append(" ").append(getHouseNum());
        if(getFraction() != null && withFraction) billingAddress.append("/").append(getFraction());
        if(getLetter() != null) billingAddress.append(getLetter());
        if(getBuild() != null) billingAddress.append("_").append(getBuild());
        billingAddress.append("-");
        if(getApartmentNum() != null) billingAddress.append(getApartmentNum());
        return billingAddress.toString();
    }

    public String getBillingAddress(){
        return getBillingAddress(false);
    }

    public void setCityByName(String cityName) {
        this.city = City.builder().name(cityName).deleted(false).build();
    }

    public String getAddressName() {
        StringBuilder addressName = new StringBuilder();
        String streetName = getStreetNamePart();
        String houseName = getHouseNamePart();
        String apartmentName = getApartmentNamePart();
        if(!streetName.isBlank()) {
            addressName.append(streetName);
        }
        if(!houseName.isBlank()) {
            addressName.append(" ").append(houseName);
        }
        if(!apartmentName.isBlank()) {
            addressName.append(" ").append(apartmentName);
        }
        return addressName.toString();
    }

    public String getHouseName() {
        StringBuilder addressName = new StringBuilder();
        String streetName = getStreetNamePart();
        String houseName = getHouseNamePart();
        if(!streetName.isBlank()) {
            addressName.append(streetName);
        }
        if(!houseName.isBlank()) {
            addressName.append(" ").append(houseName);
        }
        return addressName.toString();
    }

    public String getStreetNamePart(){
        StringBuilder part = new StringBuilder();
        if(city != null) {
            part.append(city.getName(), 0, 4).append(".");
        }
        if(street  != null) {
            part.append(" ");
            if(street.getPrefix() != null){
                part.append(street.getPrefix()).append(".");
            }
            if(street.getName() != null){
                part.append(street.getName());
            }
        }
        return part.toString();
    }

    public String getHouseNamePart(){
        StringBuilder part = new StringBuilder();
        if(houseNum != null) {
            part.append(houseNum);
        }
        if(fraction != null) {
            part.append("/").append(fraction);
        }
        if(letter != null) {
            part.append(letter);
        }
        if(build != null) {
            part.append(" стр.").append(build);
        }
        if(apartmentMod != null) {
            part.append(" (").append(apartmentMod).append(")");
        }
        return part.toString();
    }


    public String getApartmentNamePart(){
        StringBuilder part = new StringBuilder();
        if(apartmentNum != null) {
            part.append(apartmentNum);
        }
        if(entrance != null) {
            part.append(" под.").append(entrance);
        }
        if(floor != null) {
            part.append(" эт.").append(floor);
        }
        return part.toString();
    }

    public void setAddressName(String addressName) {
    }

    public void setHouse(House house){
        this.houseId = house.getHouseId();
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
