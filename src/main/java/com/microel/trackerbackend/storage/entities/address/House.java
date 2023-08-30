package com.microel.trackerbackend.storage.entities.address;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.misc.AbstractForm;
import com.microel.trackerbackend.storage.entities.Place;
import com.microel.trackerbackend.storage.entities.acp.AcpHouse;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

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
    @Nullable
    private Character letter;
    @Nullable
    private Short fraction;
    @Column(columnDefinition = "boolean default false")
    private Boolean isApartmentHouse;
    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private AcpHouse acpHouseBind;
    @Nullable
    private Short build;
    @Nullable
    @Column(columnDefinition = "boolean default false")
    private Boolean deleted;
    @ManyToOne
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Street street;

    @Nullable
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "place_id")
    private Place place;

    public String getHouseName() {
        StringBuilder sb = new StringBuilder();
        sb.append(houseNum);
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

    public String getAddressName() {
        return getAddress().getAddressName();
    }

    public Long getStreetId() {
        return street.getStreetId();
    }

    @JsonIgnore
    public boolean isSomeDeleted() {
        try {
            return Boolean.TRUE.equals(getDeleted()) || Boolean.TRUE.equals(street.getDeleted()) || Boolean.TRUE.equals(street.getCity().getDeleted());
        } catch (Exception e) {
            return true;
        }
    }

    @JsonIgnore
    public Address getAddress(@Nullable Short entrance, @Nullable Short floor, @Nullable Short apartmentNum, @Nullable String apartmentMod) {
        Address address = new Address();
        address.setCity(street.getCity());
        address.setStreet(street);
        address.setHouse(this);
        address.setEntrance(entrance);
        address.setFloor(floor);
        address.setApartmentNum(apartmentNum);
        address.setApartmentMod(apartmentMod);
        address.setAcpHouseBind(acpHouseBind);
        return address;
    }

    @JsonIgnore
    public Address getAddress() {
        Address address = new Address();
        address.setCity(street.getCity());
        address.setStreet(street);
        address.setHouse(this);
        address.setAcpHouseBind(acpHouseBind);
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

    @Getter
    @Setter
    public static class Form implements AbstractForm {
        private Short houseNum;
        @Nullable
        private Character letter;
        @Nullable
        private Short fraction;
        @Nullable
        private Short build;
        private Boolean isApartmentHouse;
        @Nullable
        private AcpHouse acpHouseBind;

        @Nullable
        private Place.Form place;

        @Override
        public boolean isValid() {
            return houseNum != null && houseNum > 0 && (fraction == null || fraction > 0) && (build == null || build > 0) && isApartmentHouse != null;
        }

        public boolean isFullEqual(House house) {
            return Objects.equals(house.houseNum, houseNum) && Objects.equals(house.letter, letter)
                    && Objects.equals(house.fraction, fraction) && Objects.equals(house.build, build)
                    && Objects.equals(house.isApartmentHouse, isApartmentHouse)
                    && Objects.equals(house.place, place == null ? null : place.toPlace())
                    && Objects.equals(house.acpHouseBind == null ? null : house.acpHouseBind.getBuildingId(), acpHouseBind == null ? null : acpHouseBind.getBuildingId());
        }
    }
}
