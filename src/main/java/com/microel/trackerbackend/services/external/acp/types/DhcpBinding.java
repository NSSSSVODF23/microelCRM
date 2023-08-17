package com.microel.trackerbackend.services.external.acp.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microel.trackerbackend.misc.network.NetworkRemoteControl;
import com.microel.trackerbackend.misc.network.NetworkState;
import lombok.*;
import org.springframework.lang.Nullable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DhcpBinding {
    private Integer id;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Short state;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Short bindtype;
    private String macaddr;
    private Integer vlanid;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer nid;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String nidSlot;
    private String ipaddr;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Short netmask;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String gw;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String dhcpRelayid;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Short dhcpPortid;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String dhcpClient;
    private String dhcpHostname;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Short dhcpFlags;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer dhcpRoutesid;
    private Integer leaseStart;
    private Integer leaseExpire;
    private Integer sessionTime;
    private String authName;
    private Integer authDate;
    private Integer authExpire;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String hashdata;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer natRuleid;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String description;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String scriptname;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String bindGroup;
    private Integer creationTime;

    public Long getLeaseStart() {
        return leaseStart * 1000L;
    }

    public Long getLeaseExpire() {
        return leaseExpire * 1000L;
    }

    public Long getSessionTime() {
        return sessionTime * 1000L;
    }

    public Long getAuthDate() {
        return authDate * 1000L;
    }

    public Long getAuthExpire() {
        return authExpire * 1000L;
    }

    public Long getCreationTime() {
        return creationTime * 1000L;
    }

    public Boolean getIsAuth() {
        return authExpire != null && authExpire > 0;
    }

    public NetworkState getOnlineStatus() {
        if (state == null || state != 1) {
            return NetworkState.OFFLINE;
        }
        return NetworkState.ONLINE;
    }
}
