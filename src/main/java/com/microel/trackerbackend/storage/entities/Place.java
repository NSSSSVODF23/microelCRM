package com.microel.trackerbackend.storage.entities;

import com.microel.trackerbackend.misc.AbstractForm;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import lombok.*;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "places")
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;
    private Double latitude;
    private Double longitude;

    @Getter
    @Setter
    public static class Form implements AbstractForm{
        private Double latitude;
        private Double longitude;

        @Override
        public boolean isValid() {
            return latitude != null && (latitude >= -90 && latitude <= 90) && longitude != null && (longitude >= -180 && longitude <= 180);
        }

        public Place toPlace(){
            if(!isValid()){
                throw new IllegalFields("Координаты должны быть в диапазоне от -90 до 90 и от -180 до 180");
            }
            return Place.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Place place)) return false;
        return Objects.equals(getLatitude(), place.getLatitude()) && Objects.equals(getLongitude(), place.getLongitude());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLatitude(), getLongitude());
    }
}
