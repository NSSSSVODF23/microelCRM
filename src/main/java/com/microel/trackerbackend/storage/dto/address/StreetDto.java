package com.microel.trackerbackend.storage.dto.address;

import com.microel.trackerbackend.parsers.addresses.StreetResponse;
import com.microel.trackerbackend.storage.entities.address.Street;
import lombok.*;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class StreetDto {
    private Long streetId;
    private String name;
    private String prefix;
    private Boolean deleted;
    private String billingAlias;

    @Nullable
    public static StreetDto of(@Nullable Street street) {
        if(street == null) return null;
        return StreetDto.builder()
                .streetId(street.getStreetId())
                .name(street.getName())
                .prefix(street.getPrefix())
                .deleted(street.getDeleted())
                .billingAlias(street.getBillingAlias())
                .build();
    }
}
