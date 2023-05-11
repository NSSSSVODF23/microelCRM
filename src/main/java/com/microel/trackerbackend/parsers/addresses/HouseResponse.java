package com.microel.trackerbackend.parsers.addresses;

import com.microel.trackerbackend.storage.entities.address.House;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HouseResponse {
    private Boolean actual;
    private UUID actualHouseGuid;
    private String additionalName;
    private Boolean aggregated;
    private UUID aoGuid;
    private Integer buildingNumber;
    private String classType;
    private String code;
    private String estStatus;
    private UUID guid;
    private String houseCondition;
    private String houseGuid;
    private String houseNumber;
    private String houseTextAddress;
    private Boolean isAddedManually;
    private String postalCode;
    private String strStatus;
    private Integer structNumber;

    @Nullable
    public House toHouse() {
        Short houseNum = null;
        Short fraction = null;
        Character letter = null;
        Short build = null;
        Pattern houseNumberPattern = Pattern.compile("^(\\d+)");
        Matcher houseNumberMatcher = houseNumberPattern.matcher(getHouseTextAddress());
        if (houseNumberMatcher.find()) {
            houseNum = Short.parseShort(houseNumberMatcher.group(1));
        }else{
            return null;
        }
        Pattern fractionPattern = Pattern.compile("^\\d+/(\\d+)");
        Matcher fractionMatcher = fractionPattern.matcher(getHouseTextAddress());
        if (fractionMatcher.find()) {
            fraction = Short.parseShort(fractionMatcher.group(1));
        }
        Pattern letterPattern = null;
        if(fraction != null){
            letterPattern = Pattern.compile("^\\d+/\\d+([а-я])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }else{
            letterPattern = Pattern.compile("^\\d+([а-я])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        Matcher letterMatcher = letterPattern.matcher(getHouseTextAddress());
        if (letterMatcher.find()) {
            letter = letterMatcher.group(1).toLowerCase().charAt(0);
        }
        Pattern buildPattern = Pattern.compile("(корпус | строение) (\\d+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher buildMatcher = buildPattern.matcher(getHouseTextAddress());
        if (buildMatcher.find()) {
            build = Short.parseShort(buildMatcher.group(2));
        }
        return House.builder()
                .houseNum(houseNum)
                .fraction(fraction)
                .letter(letter)
                .build(build)
                .build();
    }
}
