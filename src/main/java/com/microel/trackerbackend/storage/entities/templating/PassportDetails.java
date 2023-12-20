package com.microel.trackerbackend.storage.entities.templating;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "passport_details")
public class PassportDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passportDetailsId;
    private String passportSeries;
    private String passportNumber;
    private String passportIssuedBy;
    private Timestamp passportIssuedDate;
    private String departmentCode;
    private String registrationAddress;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PassportDetails that)) return false;
        return Objects.equals(getPassportDetailsId(), that.getPassportDetailsId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPassportDetailsId());
    }

    @Override
    public String toString() {
        if(passportSeries == null || passportSeries.isBlank() || passportNumber == null || passportNumber.isBlank())
            return "-";
        return passportSeries + "-" + passportNumber;
    }
}
