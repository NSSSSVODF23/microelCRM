package com.microel.trackerbackend.modules.transport.ip;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@JsonSerialize(using = IpSerializer.class)
@JsonDeserialize(using = IpDeserializer.class)
public class IP {
    private List<Short> octets;

    public IP(String ip) throws Exception {
        octets = validateAndGetOctets(ip);
    }

    public static IP of(String value) throws Exception {
        return new IP(value);
    }

    private List<Short> validateAndGetOctets(String value) throws Exception {
        List<Short> octets = new ArrayList<Short>();
        String[] splitIp = value.split("\\.");
        if(splitIp.length==4){
            for (String ip : splitIp) {
                short octet;
                try {
                    octet = Short.parseShort(ip);
                    if(octet < 0 || octet > 255) throw new Exception("Значение ip адреса за приделами доступного");
                }catch (NumberFormatException e) {
                    throw new Exception("Не верный формат ip адреса");
                }
                octets.add(octet);
            }
        }else{
            throw new Exception("Количество октетов в ip адресе не верно");
        }
        return octets;
    }

    @Override
    public String toString() {
        return octets.stream().map(Object::toString).collect(Collectors.joining("."));
    }
}
