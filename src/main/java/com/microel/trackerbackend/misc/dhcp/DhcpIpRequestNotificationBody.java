package com.microel.trackerbackend.misc.dhcp;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DhcpIpRequestNotificationBody {
    private String ip;
    private String mac;
    private Integer vlan;
    private String device;
    private String owner;
    private List<SwitchInfo> switches;

    @Getter
    @Setter
    public static class SwitchInfo {
        private String name;
        private String street;
        private String house;
        private String model;
        private String ipaddress;
    }
}
