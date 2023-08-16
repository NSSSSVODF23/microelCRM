package com.microel.trackerbackend.services.external.types.acp;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Network {
    private Integer id;
    private Integer vid;
    private String network;
    private Short nettype;
    private Short dhcpSubnets;
    private Short dhcpCustomMask;
    private String dhcpCustomGw;
    private Short dhcpOpt82Autodetect;
    private String dhcpRelayIp;
    private String description;
    private Short status;
    private String dhcpRoute;
    private Short dhcpValDefaults;

}
