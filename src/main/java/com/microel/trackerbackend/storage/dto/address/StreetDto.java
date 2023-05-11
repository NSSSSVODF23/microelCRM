package com.microel.trackerbackend.storage.dto.address;

import com.microel.trackerbackend.parsers.addresses.StreetResponse;
import lombok.*;

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
}
