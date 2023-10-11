package com.microel.trackerbackend.parsers.commutator.parsers;

import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HuaweiParser {
    public static SystemInfo parseSiOldHu(String data) {
        Pattern deviceAndUptimePattern = Pattern.compile("^(?<model>[A-z\\d-\\s]{3,40})\\suptime is\\s(?<uptime>[^\\r\\n]+)$", Pattern.MULTILINE);
        Pattern fwVersionPattern = Pattern.compile("Version ([\\d\\.]{1,5})");
        Pattern hwVersionPattern = Pattern.compile("^Pcb      Version :\\s+([^\\r\\n]+)$", Pattern.MULTILINE);

        Matcher deviceAndUptimeMatcher = deviceAndUptimePattern.matcher(data);
        Matcher fwVersionMatcher = fwVersionPattern.matcher(data);
        Matcher hwVersionMatcher = hwVersionPattern.matcher(data);

        SystemInfo result = new SystemInfo();


        if (deviceAndUptimeMatcher.find()) {
            result.setDevice(deviceAndUptimeMatcher.group("model"));
            result.setUptime(convertUptime(deviceAndUptimeMatcher.group("uptime")));
        }
        if (fwVersionMatcher.find()) {
            result.setFwVersion(fwVersionMatcher.group(1));
        }
        if (hwVersionMatcher.find()) {
            result.setHwVersion(hwVersionMatcher.group(1));
        }

        return result;
    }

    public static SystemInfo parseSiHu(String data) {
        Pattern deviceAndUptimePattern = Pattern.compile("^(?<model>[A-z\\d-\\s]{3,40})\\suptime is\\s(?<uptime>[^\\r\\n]+)$", Pattern.MULTILINE);
        Pattern fwVersionPattern = Pattern.compile("Version ([\\d\\.]{1,5})");
        Pattern hwVersionPattern = Pattern.compile("^Hardware Version is (.+)$", Pattern.MULTILINE);

        Matcher deviceAndUptimeMatcher = deviceAndUptimePattern.matcher(data);
        Matcher fwVersionMatcher = fwVersionPattern.matcher(data);
        Matcher hwVersionMatcher = hwVersionPattern.matcher(data);

        SystemInfo result = new SystemInfo();

        if (deviceAndUptimeMatcher.find()) {
            result.setDevice(deviceAndUptimeMatcher.group("model"));
            result.setUptime(convertUptime(deviceAndUptimeMatcher.group("uptime")));
        }else{
            throw new ParsingException("Не удалось получить информацию о коммутаторе (Название устройства, аптайм)");
        }
        if (fwVersionMatcher.find()) {
            result.setFwVersion(fwVersionMatcher.group(1));
        }else{
            throw new ParsingException("Не удалось получить информацию о коммутаторе (Версия прошивки)");
        }
        if (hwVersionMatcher.find()) {
            result.setHwVersion(hwVersionMatcher.group(1));
        }else{
            throw new ParsingException("Не удалось получить информацию о коммутаторе (Ревизия)");
        }

        return result;
    }

    public static List<PortInfo> parsePortsOldHu(String data, String fdbData) {
        List<PortInfo> result = new ArrayList<>();
        String[] interfaces = data.split("    Output bandwidth utilization :\\s+\\d+.\\d+%[\\r\\n]+[\\r\\n]+");
        Pattern interfaceStatusPattern = Pattern.compile("(?<type>Ethernet|GigabitEthernet)\\d/\\d/\\d{1,2} current state : (?<state>(UP|DOWN|Administratively DOWN))");
        Pattern interfaceHWPattern = Pattern.compile("Current Work Mode: FIBER");
        Pattern interfaceSpeedPattern = Pattern.compile("Speed :\\s+(\\d{2,4}),");
        Pattern interfaceDuplexPattern = Pattern.compile("Duplex:\\s+(FULL|HALF),");
        Pattern interfaceForcePattern = Pattern.compile("Negotiation:\\s+(ENABLE|DISABLE)");
        Pattern interfaceDescPattern = Pattern.compile("Description:\\s*([^\\r\\n]+)");
        Pattern fdbPattern = Pattern.compile("(?<mac>[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}) (?<vlan>\\d{1,4})/-\\s+(?<ifName>(GE|Eth))\\d/\\d/(?<ifIdx>\\d{1,2})\\s+(?<dy>\\w+)");

        int ethLast = 0;

        for (int i = 0; i < interfaces.length; i++) {
            String interfaceData = interfaces[i];
            Matcher interfaceStatusMatcher = interfaceStatusPattern.matcher(interfaceData);
            Matcher interfaceSpeedMatcher = interfaceSpeedPattern.matcher(interfaceData);
            Matcher interfaceDuplexMatcher = interfaceDuplexPattern.matcher(interfaceData);
            Matcher interfaceForceMatcher = interfaceForcePattern.matcher(interfaceData);
            Matcher interfaceDescMatcher = interfaceDescPattern.matcher(interfaceData);

            if (!interfaceStatusMatcher.find()) continue;
            PortInfo portInfo = new PortInfo();
            portInfo.setName(String.valueOf(i+1));
            portInfo.setPortType(PortInfo.PortType.COPPER);
            portInfo.setMacTable(new ArrayList<>());

            if(interfaceHWPattern.matcher(interfaceData).find())
                portInfo.setPortType(PortInfo.PortType.FIBER);

            switch (interfaceStatusMatcher.group("state")){
                case "UP" -> portInfo.setStatus(PortInfo.Status.UP);
                case "DOWN" -> portInfo.setStatus(PortInfo.Status.DOWN);
                case "Administratively DOWN" -> portInfo.setStatus(PortInfo.Status.ADMIN_DOWN);
                default -> portInfo.setStatus(PortInfo.Status.DOWN);
            }

            if(interfaceSpeedMatcher.find() && interfaceDuplexMatcher.find() && portInfo.getStatus().equals(PortInfo.Status.UP)){
                if(interfaceDuplexMatcher.group(1).equals("FULL")){
                    switch (interfaceSpeedMatcher.group(1)){
                        case "10" -> portInfo.setSpeed(PortInfo.Speed.FULL10);
                        case "100" -> portInfo.setSpeed(PortInfo.Speed.FULL100);
                        case "1000" -> portInfo.setSpeed(PortInfo.Speed.FULL1000);
                    }
                }else{
                    switch (interfaceSpeedMatcher.group(1)){
                        case "10" -> portInfo.setSpeed(PortInfo.Speed.HALF10);
                        case "100" -> portInfo.setSpeed(PortInfo.Speed.HALF100);
                        case "1000" -> portInfo.setSpeed(PortInfo.Speed.HALF1000);
                    }
                }
            }else{
                portInfo.setSpeed(null);
            }

            if(interfaceForceMatcher.find()){
                if(interfaceForceMatcher.group(1).equals("DISABLE"))
                    portInfo.setForce(true);
            }

            if(interfaceDescMatcher.find()){
                portInfo.setDescription(interfaceDescMatcher.group(1));
            }

            switch (interfaceStatusMatcher.group("type")){
                case "Ethernet" -> {
                    portInfo.setType(PortInfo.InterfaceType.ETHERNET);
                    ethLast = Math.max(ethLast, i+1);
                }
                case "GigabitEthernet" -> portInfo.setType(PortInfo.InterfaceType.GIGABIT);
            }

            result.add(portInfo);
        }

        Matcher fdbMatcher = fdbPattern.matcher(fdbData);

        while (fdbMatcher.find()) {

            try {

                String macRaw = fdbMatcher.group("mac").replaceAll("-", "").toLowerCase().trim();
                StringBuilder mac = new StringBuilder();
                for(int i = 0; i < 5; i++){
                    mac.append(macRaw.substring(i * 2, i * 2 + 2)).append(":");
                }

                String ifName = fdbMatcher.group("ifName");
                int ifIdx = Integer.parseInt(fdbMatcher.group("ifIdx"));
                mac.append(macRaw.substring(5*2));

                if(ifName.equals("GE"))
                    ifIdx += ethLast;

                FdbItem fdbItem = FdbItem.builder()
                        .portId(ifIdx)
                        .mac(mac.toString())
                        .dynamic(fdbMatcher.group("dy").equals("dynamic"))
                        .vid(Integer.parseInt(fdbMatcher.group("vlan")))
                        .vlanName("Нет имени")
                        .build();

                result.get(ifIdx-1).appendToMacTable(fdbItem);
            } catch (Throwable e) {
                throw new ParsingException("Ошибка чтения FDB таблицы: " + e.getMessage());
            }
        }

        return result;
    }

    public static List<PortInfo> parsePortsHu(List<String> interfaces, List<String> fdbData) {
        List<PortInfo> result = new ArrayList<>();

        Pattern interfaceStatusPattern = Pattern.compile("(?<type>Ethernet|GigabitEthernet)\\d/\\d/\\d{1,2} current state : (?<state>(UP|DOWN|ADMINISTRATIVELY DOWN))");
        Pattern interfaceHWPattern = Pattern.compile("Media type is optical fiber");
        Pattern interfaceSpeedDuplexPattern = Pattern.compile("(?<speed>\\d{2,4})Mbps-speed mode, (?<duplex>full|half)-duplex mode");
        Pattern interfaceForcePattern = Pattern.compile("Link speed type is autonegotiation, link duplex type is autonegotiation");
        Pattern interfaceDescPattern = Pattern.compile("Description:\\s*([^\\r\\n]+)");
        Pattern fdbPattern = Pattern.compile("(?<mac>[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4})\\s+(?<vlan>\\d{1,4})\\s+\\w+\\s+(?<ifName>(GigabitEthernet|Ethernet))\\d/\\d/(?<ifIdx>\\d{1,2})\\s+\\w+");

        int ethLast = 0;

        for (int i = 0; i < interfaces.size(); i++) {
            String interfaceData = interfaces.get(i);
            Matcher interfaceStatusMatcher = interfaceStatusPattern.matcher(interfaceData);
            Matcher interfaceSpeedDuplexMatcher = interfaceSpeedDuplexPattern.matcher(interfaceData);
            Matcher interfaceForceMatcher = interfaceForcePattern.matcher(interfaceData);
            Matcher interfaceDescMatcher = interfaceDescPattern.matcher(interfaceData);

            if (!interfaceStatusMatcher.find()) continue;
            PortInfo portInfo = new PortInfo();
            portInfo.setName(String.valueOf(i+1));
            portInfo.setPortType(PortInfo.PortType.COPPER);

            Matcher fdbMatcher = fdbPattern.matcher(fdbData.get(i));
            ArrayList<FdbItem> fdbTable = new ArrayList<>();

            while (fdbMatcher.find()) {
                try {
                    String macRaw = fdbMatcher.group("mac").replaceAll("-", "").toLowerCase().trim();
                    StringBuilder mac = new StringBuilder();
                    for(int chi = 0; chi < 5; chi++){
                        mac.append(macRaw.substring(chi * 2, chi * 2 + 2)).append(":");
                    }

                    String ifName = fdbMatcher.group("ifName");
                    int ifIdx = Integer.parseInt(fdbMatcher.group("ifIdx"));
                    mac.append(macRaw.substring(5*2));

                    if(ifName.equals("GigabitEthernet"))
                        ifIdx += ethLast;

                    FdbItem fdbItem = FdbItem.builder()
                            .portId(ifIdx)
                            .mac(mac.toString())
                            .dynamic(true)
                            .vid(Integer.parseInt(fdbMatcher.group("vlan")))
                            .vlanName("Нет имени")
                            .build();

                    fdbTable.add(fdbItem);
                } catch (Throwable e) {
                    throw new ParsingException("Ошибка чтения FDB таблицы: " + e.getMessage());
                }
            }

            portInfo.setMacTable(fdbTable);

            if(interfaceHWPattern.matcher(interfaceData).find())
                portInfo.setPortType(PortInfo.PortType.FIBER);

            switch (interfaceStatusMatcher.group("state")){
                case "UP" -> portInfo.setStatus(PortInfo.Status.UP);
                case "DOWN" -> portInfo.setStatus(PortInfo.Status.DOWN);
                case "ADMINISTRATIVELY DOWN" -> portInfo.setStatus(PortInfo.Status.ADMIN_DOWN);
                default -> portInfo.setStatus(PortInfo.Status.DOWN);
            }

            if(interfaceSpeedDuplexMatcher.find() && portInfo.getStatus().equals(PortInfo.Status.UP)){
                if(interfaceSpeedDuplexMatcher.group("duplex").equals("full")){
                    switch (interfaceSpeedDuplexMatcher.group("speed")){
                        case "10" -> portInfo.setSpeed(PortInfo.Speed.FULL10);
                        case "100" -> portInfo.setSpeed(PortInfo.Speed.FULL100);
                        case "1000" -> portInfo.setSpeed(PortInfo.Speed.FULL1000);
                    }
                }else{
                    switch (interfaceSpeedDuplexMatcher.group("speed")){
                        case "10" -> portInfo.setSpeed(PortInfo.Speed.HALF10);
                        case "100" -> portInfo.setSpeed(PortInfo.Speed.HALF100);
                        case "1000" -> portInfo.setSpeed(PortInfo.Speed.HALF1000);
                    }
                }
            }else{
                portInfo.setSpeed(null);
            }

            if(!interfaceForceMatcher.find()){
                portInfo.setForce(true);
            }

            if(interfaceDescMatcher.find()){
                portInfo.setDescription(interfaceDescMatcher.group(1));
            }

            switch (interfaceStatusMatcher.group("type")){
                case "Ethernet" -> {
                    portInfo.setType(PortInfo.InterfaceType.ETHERNET);
                    ethLast = Math.max(ethLast, i+1);
                }
                case "GigabitEthernet" -> portInfo.setType(PortInfo.InterfaceType.GIGABIT);
            }

            result.add(portInfo);
        }

        return result;
    }


    @Nullable
    public static Integer convertUptime(String uptime) {
        Pattern convertPattern = Pattern.compile("((?<weeks>\\d{1,3}) weeks?, )?((?<days>\\d{1,3}) days?, )?((?<hours>\\d{1,3}) hours?, )?(?<minutes>\\d{1,3}) minutes?");
        Matcher convertMatcher = convertPattern.matcher(uptime);

        if(!convertMatcher.find()) return null;

        int weeks = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
        try {
            weeks = Integer.parseInt(convertMatcher.group("weeks"));
        } catch (IllegalArgumentException ignore) {
        }
        try {
            days = Integer.parseInt(convertMatcher.group("days"));
        } catch (IllegalArgumentException ignore) {
        }
        try {
            hours = Integer.parseInt(convertMatcher.group("hours"));
        } catch (IllegalArgumentException ignore) {
        }
        try {
            minutes = Integer.parseInt(convertMatcher.group("minutes"));
        } catch (IllegalArgumentException ignore) {
        }
        seconds += minutes * 60;
        seconds += hours * 3600;
        seconds += days * 86400;
        seconds += weeks * 604800;

        return seconds;
    }
}
