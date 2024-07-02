package com.microel.trackerbackend.misc.dhcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class VpnStateNotificationBody {
    private Boolean state;
    @JsonProperty("src_ip")
    private String srcIp;
    @JsonProperty("self_ip")
    @Nullable
    private String selfIp;
    @Nullable
    private String connection;
    @Nullable
    private String traffic;
}
