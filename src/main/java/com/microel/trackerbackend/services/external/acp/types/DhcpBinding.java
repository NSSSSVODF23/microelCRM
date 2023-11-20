package com.microel.trackerbackend.services.external.acp.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microel.trackerbackend.controllers.telegram.TelegramMessageFactory;
import com.microel.trackerbackend.controllers.telegram.handle.Decorator;
import com.microel.trackerbackend.misc.network.NetworkRemoteControl;
import com.microel.trackerbackend.misc.network.NetworkState;
import com.microel.trackerbackend.services.external.billing.BillingRequestController;
import com.microel.trackerbackend.storage.entities.acp.NetworkConnectionLocation;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import lombok.*;
import net.time4j.Moment;
import net.time4j.PrettyTime;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

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
    @Nullable
    private Integer streetId;
    @Nullable
    private String streetName;
    @Nullable
    private Integer buildingId;
    @Nullable
    private String houseNum;
    @Nullable
    private NetworkConnectionLocation lastConnectionLocation;
    @Nullable
    private String billingAddress;

    @JsonIgnore
    public String getTextRow(){
        PrettyTime prettyTime = PrettyTime.of(Locale.forLanguageTag("ru-RU"));
        String sessionDuration = prettyTime.printRelativeInStdTimezone(Moment.from(Instant.ofEpochMilli(getLeaseStart())));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Decorator.bold(getMacaddr()));
        stringBuilder.append(" ").append(getAuthName());
        if(getBillingAddress() != null){
            stringBuilder.append("\n").append(getBillingAddress());
        }
        stringBuilder.append("\n").append(Decorator.bold(getIpaddr()));
        stringBuilder.append(" ").append(getDhcpHostname());
        stringBuilder.append("\n").append(Decorator.italic(sessionDuration));
        if(getLastConnectionLocation() != null && getLastConnectionLocation().getLastPortCheck() != null){
            String updateDuration = prettyTime.printRelativeInStdTimezone(Moment.from(getLastConnectionLocation().getLastPortCheck().toInstant()));
            stringBuilder.append("\n").append("Был подключен к коммутатору:");
            stringBuilder.append("\n")
                    .append(getLastConnectionLocation().getCommutatorName())
                    .append(" Порт ")
                    .append(getLastConnectionLocation().getPortName())
                    .append("\n").append(Decorator.italic(updateDuration));
        }
        return stringBuilder.toString();
    }

    @JsonIgnore
    public String getTextRowWithOnline(){
        PrettyTime prettyTime = PrettyTime.of(Locale.forLanguageTag("ru-RU"));
        String sessionDuration = prettyTime.printRelativeInStdTimezone(Moment.from(Instant.ofEpochMilli(getLeaseStart())));
        StringBuilder stringBuilder = new StringBuilder();
        if(getOnlineStatus() == NetworkState.ONLINE){
            stringBuilder.append(Decorator.bold("Онлайн"));
        }else{
            stringBuilder.append("Офлайн");
        }
        stringBuilder.append("\n").append(Decorator.bold(getMacaddr()));
        stringBuilder.append(" ").append(getAuthName());
        if(getBillingAddress() != null){
            stringBuilder.append("\n").append(getBillingAddress());
        }
        stringBuilder.append("\n").append(Decorator.bold(getIpaddr()));
        stringBuilder.append(" ").append(getDhcpHostname());
        stringBuilder.append("\n").append(Decorator.italic(sessionDuration));
        if(getLastConnectionLocation() != null && getLastConnectionLocation().getLastPortCheck() != null){
            String updateDuration = prettyTime.printRelativeInStdTimezone(Moment.from(getLastConnectionLocation().getLastPortCheck().toInstant()));
            stringBuilder.append("\n").append("Был подключен к коммутатору:");
            stringBuilder.append("\n")
                    .append(getLastConnectionLocation().getCommutatorName())
                    .append(" Порт ")
                    .append(getLastConnectionLocation().getPortName())
                    .append("\n").append(Decorator.italic(updateDuration));
        }
        return stringBuilder.toString();
    }

    public Long getLeaseStart() {
        if(leaseStart == null){
            return 0L;
        }
        return leaseStart * 1000L;
    }

    public Long getLeaseExpire() {
        if(leaseExpire == null){
            return 0L;
        }
        return leaseExpire * 1000L;
    }

    public Long getSessionTime() {
        if(sessionTime == null){
            return 0L;
        }
        return sessionTime * 1000L;
    }

    public Long getAuthDate() {
        if(authDate == null){
            return 0L;
        }
        return authDate * 1000L;
    }

    public Long getAuthExpire() {
        if(authExpire == null){
            return 0L;
        }
        return authExpire * 1000L;
    }

    public Long getCreationTime() {
        if(creationTime == null){
            return 0L;
        }
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

    @Getter
    @Setter
    public static class AuthForm{
        private String login;
        private String macaddr;

        public void setLogin(String login){
            Pattern p = Pattern.compile("^[a-zA-Z0-9]+$");
            if(!p.matcher(login).matches()){
                throw new IllegalFields("Не верный логин");
            }
            this.login = login;
        }

        public void setMacaddr(String macaddr){
            String trimmed = macaddr.trim().replaceAll("[^0-9A-f]", "").toLowerCase();
            if(!trimmed.matches("[0-9a-f]{12}")){
                throw new IllegalFields("Не верный MAC-адрес");
            }
            StringBuilder collectedBack = new StringBuilder();
            char[] chars = trimmed.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                collectedBack.append(chars[i]);
                if(i % 2 == 1 && i != chars.length - 1){
                    collectedBack.append(":");
                }
            }
            this.macaddr = collectedBack.toString();
        }
    }
}
