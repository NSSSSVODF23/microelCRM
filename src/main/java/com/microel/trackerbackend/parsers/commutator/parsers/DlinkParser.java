package com.microel.trackerbackend.parsers.commutator.parsers;

import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DlinkParser {
    public static SystemInfo parseSIDes28(String data) {
        Pattern devicePattern = Pattern.compile("Device Type\\s+: ([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);
        Pattern macPattern = Pattern.compile("MAC Address\\s+: ([\\dA-F\\-]{17})", Pattern.CASE_INSENSITIVE);
        Pattern hwVersionPattern = Pattern.compile("Hardware Version\\s+: ([\\w]+)", Pattern.CASE_INSENSITIVE);
        Pattern fwVersionPattern = Pattern.compile("Firmware Version\\s+: ([\\w\\. ]+)", Pattern.CASE_INSENSITIVE);
        Pattern uptimePattern = Pattern.compile("System up ?time\\s+: ([^\\n]+)", Pattern.CASE_INSENSITIVE);

        Matcher deviceMatcher = devicePattern.matcher(data);
        Matcher macMatcher = macPattern.matcher(data);
        Matcher hwVersionMatcher = hwVersionPattern.matcher(data);
        Matcher fwVersionMatcher = fwVersionPattern.matcher(data);
        Matcher uptimeMatcher = uptimePattern.matcher(data);

        SystemInfo result = new SystemInfo();

        if (deviceMatcher.find()) {
            result.setDevice(deviceMatcher.group(1));
        }

        if (macMatcher.find()) {
            result.setMac(macMatcher.group(1).toLowerCase().replaceAll("-", ":"));
        }

        if (hwVersionMatcher.find()) {
            result.setHwVersion(hwVersionMatcher.group(1));
        }

        if (fwVersionMatcher.find()) {
            result.setFwVersion(fwVersionMatcher.group(1));
        }

        if (uptimeMatcher.find()) {
            result.setUptime(convertUptime(uptimeMatcher.group(1)));
        }

        return result;
    }

    public static SystemInfo parseSIDgs8(String constInfo, String switchInfo) {
        Pattern deviceConstantInfoPattern = Pattern.compile("var g_SwitchInfo=\\[([^\\]]+)\\];");
        Pattern deviceSwitchInfoPattern = Pattern.compile("var g_DeviceInfo=\\[([^\\]]+)\\];");
        Pattern deviceUptimeInfoPattern = Pattern.compile("var ds_TimeUp=\\[([^\\]]+)\\];");
        Matcher deviceConstantInfoMatcher = deviceConstantInfoPattern.matcher(constInfo);
        Matcher deviceSwitchInfoMatcher = deviceSwitchInfoPattern.matcher(switchInfo);
        Matcher deviceUptimeInfoMatcher = deviceUptimeInfoPattern.matcher(switchInfo);

        if (!deviceConstantInfoMatcher.find())
            throw new ParsingException("Не удалось получить информацию о коммутаторе");
        if (!deviceSwitchInfoMatcher.find())
            throw new ParsingException("Не удалось получить информацию о коммутаторе");
        if (!deviceUptimeInfoMatcher.find())
            throw new ParsingException("Не удалось получить информацию о коммутаторе");

        List<String> deviceConstantInfo = Arrays.stream(deviceConstantInfoMatcher.group(1).replaceAll("'", "").split(",")).map(String::trim).toList();
        List<String> deviceSwitchInfo = Arrays.stream(deviceSwitchInfoMatcher.group(1).replaceAll("'", "").split(",")).map(String::trim).toList();
        List<String> deviceUptimeInfo = Arrays.stream(deviceUptimeInfoMatcher.group(1).replaceAll("'", "").split(",")).map(String::trim).toList();

        SystemInfo result = new SystemInfo();
        result.setDevice(deviceConstantInfo.get(0));
        result.setFwVersion(deviceConstantInfo.get(1));
        result.setHwVersion(deviceSwitchInfo.get(0));
        result.setMac(deviceConstantInfo.get(2).toLowerCase().replaceAll("-", ":"));
        result.setUptime(convertUptime(deviceUptimeInfo));

        return result;
    }

    public static SystemInfo parseSIDes16(String data) {
        Pattern deviceInfoPattern = Pattern.compile("Switch_Status=\\[([^\\]]+)\\];");
        Matcher deviceInfoMatcher = deviceInfoPattern.matcher(data);
        if (!deviceInfoMatcher.find())
            throw new ParsingException("Не удалось получить информацию о коммутаторе");

        List<String> deviceInfo = Arrays.stream(deviceInfoMatcher.group(1).replaceAll("'", "").split(",")).map(String::trim).toList();
        SystemInfo result = new SystemInfo();
        result.setDevice(deviceInfo.get(0));
        result.setMac(deviceInfo.get(3).toLowerCase().replaceAll("-", ":"));
        result.setFwVersion(deviceInfo.get(1));
        result.setHwVersion(deviceInfo.get(7));
        result.setUptime(convertUptime(deviceInfo.get(6)));
        return result;
    }

    public static List<PortInfo> parsePortsDes28(String statusData, String portTypeData, String fdbData) {
        Pattern statusPostPattern = Pattern.compile(" ?(?<name>\\d{1,2}(\\([CF]\\))?)\\s+(?<state>(Enabled|Disabled))\\s+(?<setspd>[\\w/]+)\\s+(?<speed>(Link ?Down|[\\w/]+))\\s+\\w+\\s+(\\w+|N/A)?\\s+Desc:(?<desc>.*)");
        Matcher statusPostMatcher = statusPostPattern.matcher(statusData);
        List<PortInfo> ports = new ArrayList<>();
        while (statusPostMatcher.find()) {
            try {
                PortInfo.PortInfoBuilder builder = PortInfo.builder();
                String name = statusPostMatcher.group("name");
                String state = statusPostMatcher.group("state");
                String setspd = statusPostMatcher.group("setspd");
                String speed = statusPostMatcher.group("speed");
                String desc = null;
                try {
                    desc = statusPostMatcher.group("desc").trim();
                } catch (Throwable ignore) {
                }

                builder.name(name);

                if (state.equals("Disabled")) {
                    builder.status(PortInfo.Status.ADMIN_DOWN);
                } else if (speed.equals("Link Down") || speed.equals("LinkDown")) {
                    builder.status(PortInfo.Status.DOWN);
                } else {
                    builder.status(PortInfo.Status.UP);
                    String[] split = speed.split("/");
                    if (split[1].equals("Full")) {
                        switch (split[0]) {
                            case "10M" -> builder.speed(PortInfo.Speed.FULL10);
                            case "100M" -> builder.speed(PortInfo.Speed.FULL100);
                            case "1000M" -> builder.speed(PortInfo.Speed.FULL1000);
                        }
                    } else if (split[1].equals("Half")) {
                        switch (split[0]) {
                            case "10M" -> builder.speed(PortInfo.Speed.HALF10);
                            case "100M" -> builder.speed(PortInfo.Speed.HALF100);
                            case "1000M" -> builder.speed(PortInfo.Speed.HALF1000);
                        }
                    }
                }

                String[] split = setspd.split("/");
                if (split.length == 2 && split[0].equals("Auto")) {
                    builder.force(false);
                } else {
                    builder.force(true);
                }

                builder.description(desc);
                builder.macTable(new ArrayList<>());
                PortInfo port = builder.build();
                ports.add(port);
            } catch (IllegalArgumentException e) {
                throw new ParsingException("Ошибка парсинга статуса портов");
            }
        }

        if (ports.size() == 0)
            throw new ParsingException("Ошибка парсинга портов");

        Pattern portTypePattern = Pattern.compile("^\\d{1,2}\\s+\\([CF]\\)\\s+(?<ptype>\\w+)", Pattern.MULTILINE);
        Matcher portTypeMatcher = portTypePattern.matcher(portTypeData);
        int idx = 0;
        while (portTypeMatcher.find()) {
            try {
                String portType = portTypeMatcher.group("ptype");
                PortInfo portInfo = ports.get(idx);
                switch (portType) {
                    case "100BASE" -> {
                        portInfo.setPortType(PortInfo.PortType.COPPER);
                        portInfo.setType(PortInfo.InterfaceType.ETHERNET);
                    }
                    case "1000BASE" -> {
                        portInfo.setPortType(PortInfo.PortType.COPPER);
                        portInfo.setType(PortInfo.InterfaceType.GIGABIT);
                    }
                    case "SFP" -> {
                        portInfo.setPortType(PortInfo.PortType.FIBER);
                        portInfo.setType(PortInfo.InterfaceType.GIGABIT);
                    }
                }
                idx++;
            } catch (IllegalArgumentException e) {
                throw new ParsingException("Ошибка парсинга типа портов");
            }
        }

        Pattern fdbPattern = Pattern.compile("(?<vid>\\d{1,4})\\s+(?<name>[\\w\\-_]+)\\s+(?<mac>[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2})\\s+(?<port>\\d{1,2})\\s+(?<type>\\w+)");
        Matcher fdbMatcher = fdbPattern.matcher(fdbData);
        List<FdbItem> fdbTable = new ArrayList<>();
        while (fdbMatcher.find()) {
            try {
                FdbItem fdbItem = FdbItem.builder()
                        .portId(Integer.parseInt(fdbMatcher.group("port")))
                        .mac(fdbMatcher.group("mac").toLowerCase().replaceAll("-", ":"))
                        .dynamic(fdbMatcher.group("type").equals("Dynamic"))
                        .vid(Integer.parseInt(fdbMatcher.group("vid")))
                        .vlanName(fdbMatcher.group("name"))
                        .build();
                fdbTable.add(fdbItem);
            } catch (Throwable e) {
                throw new ParsingException("Ошибка чтения FDB таблицы: " + e.getMessage());
            }
        }

        if (fdbTable.isEmpty()) return ports;

        fdbTable.sort(Comparator.comparingInt(FdbItem::getPortId));
        for (FdbItem item : fdbTable) {
            ports.stream().filter(port -> Objects.equals(port.getPortId(), item.getPortId())).findFirst().ifPresent(port -> port.appendToMacTable(item));
        }

        return ports;
    }

    public static List<PortInfo> parsePortsDes16(String portInfo, String fdbInfo) {
        List<PortInfo> ports = new ArrayList<>();

        Pattern portInfoPattern = Pattern.compile("\\[(?<portName>\\d{1,2}),(?<portStatus>\\d),(?<portSettings>\\d),\\d]");
        Matcher portInfoMatcher = portInfoPattern.matcher(portInfo);
        while (portInfoMatcher.find()) {
            String portName = portInfoMatcher.group("portName");
            String portStatus = portInfoMatcher.group("portStatus");
            String portSettings = portInfoMatcher.group("portSettings");
            PortInfo port = new PortInfo();
            port.setName(portName);
            if (portStatus.equals("0")) {
                port.setStatus(portSettings.equals("0") ? PortInfo.Status.ADMIN_DOWN : PortInfo.Status.DOWN);
            } else {
                port.setStatus(PortInfo.Status.UP);
            }
            port.setForce(!portSettings.equals("1") && !portSettings.equals("0"));
            port.setType(PortInfo.InterfaceType.ETHERNET);
            port.setPortType(PortInfo.PortType.COPPER);
            switch (portStatus) {
                case "5" -> port.setSpeed(PortInfo.Speed.FULL100);
                case "4" -> port.setSpeed(PortInfo.Speed.HALF100);
                case "3" -> port.setSpeed(PortInfo.Speed.FULL10);
                case "2" -> port.setSpeed(PortInfo.Speed.HALF10);
            }
            ports.add(port);
        }

        List<FdbItem> fdbTable = new ArrayList<>();
        Pattern fdbInfoPattern = Pattern.compile("\\[\\d{1,2},(?<portName>\\d{1,2}),'(?<mac>[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2})',(?<vid>\\d{1,4}),(?<dynamic>\\d)]");
        Matcher fdbInfoMatcher = fdbInfoPattern.matcher(fdbInfo);
        while (fdbInfoMatcher.find()) {
            FdbItem fdbItem = FdbItem.builder()
                    .portId(Integer.parseInt(fdbInfoMatcher.group("portName")))
                    .mac(fdbInfoMatcher.group("mac").toLowerCase().replaceAll("-", ":"))
                    .dynamic(fdbInfoMatcher.group("dynamic").equals("1"))
                    .vid(Integer.parseInt(fdbInfoMatcher.group("vid")))
                    .vlanName("Нет имени")
                    .build();
            fdbTable.add(fdbItem);
        }

        fdbTable.sort(Comparator.comparingInt(FdbItem::getPortId));
        for (FdbItem item : fdbTable) {
            ports.stream().filter(port -> Objects.equals(port.getPortId(), item.getPortId())).findFirst().ifPresent(port -> port.appendToMacTable(item));
        }
        return ports;
    }

    public static List<PortInfo> parsePortsDgs8(String portInfo, String fdbInfo) {
        List<PortInfo> ports = new ArrayList<>();

        Pattern portInfoPattern = Pattern.compile("var ds_PortSetting=\\[([\\[\\]\\d,\\s']+)\\];");
        Matcher portInfoMatcher = portInfoPattern.matcher(portInfo);
        if (!portInfoMatcher.find())
            throw new ParsingException("Информация о портах коммутатора не найдена");
        List<String> portsRaw = Arrays.stream(portInfoMatcher.group(1).trim().split(",\\s")).toList();
        List<List<String>> clearPorts = portsRaw.stream().map(port -> Arrays.stream(port.replaceAll("[\\[\\]]", "").split(",")).toList()).toList();

        int index = 1;

        for (List<String> port : clearPorts) {
            PortInfo portInfoItem = new PortInfo();
            portInfoItem.setName(String.valueOf(index++));
            portInfoItem.setType(PortInfo.InterfaceType.GIGABIT);
            portInfoItem.setPortType(PortInfo.PortType.COPPER);

            if (port.size() == 7) {
                String linkState = port.get(0); // 2 - up, 0 - down
                String adminDown = port.get(1); // 0 - disable, 1 - enable
                String setting = port.get(3); //  0 - Auto, 1 - 10M Half, 2 - 10M Full, 3 - 100M Half, 4 - 100M Full, 5 - 1000M Full, 6 - 1000M Full
                String speed = port.get(4); //  0 - Link Down, 1 - 10M Half, 2 - 10M Full, 3 - 100M Half, 4 - 100M Full, 5 - 1000M Full, 6 - 1000M Full
                String description = port.get(5).replaceAll("'","").trim();

                portInfoItem.setStatus(linkState.equals("2") ? PortInfo.Status.UP : (adminDown.equals("0") ? PortInfo.Status.ADMIN_DOWN : PortInfo.Status.DOWN));
                portInfoItem.setForce(!setting.equals("0"));
                switch (speed){
                    case "1" -> portInfoItem.setSpeed(PortInfo.Speed.HALF10);
                    case "2" -> portInfoItem.setSpeed(PortInfo.Speed.FULL10);
                    case "3" -> portInfoItem.setSpeed(PortInfo.Speed.HALF100);
                    case "4" -> portInfoItem.setSpeed(PortInfo.Speed.FULL100);
                    case "5", "6" -> portInfoItem.setSpeed(PortInfo.Speed.FULL1000);
                }
                portInfoItem.setDescription(description);
            }else if(port.size() == 4){
                portInfoItem.setStatus(!port.get(0).equals("0") ? PortInfo.Status.UP : port.get(1).equals("0") ? PortInfo.Status.ADMIN_DOWN : PortInfo.Status.DOWN);
                portInfoItem.setForce(!port.get(1).equals("0") && !port.get(1).equals("1"));
                switch (port.get(0)){
                    case "7" -> portInfoItem.setSpeed(PortInfo.Speed.FULL1000);
                    case "6" -> portInfoItem.setSpeed(PortInfo.Speed.HALF1000);
                    case "5" -> portInfoItem.setSpeed(PortInfo.Speed.FULL100);
                    case "4" -> portInfoItem.setSpeed(PortInfo.Speed.HALF100);
                    case "3" -> portInfoItem.setSpeed(PortInfo.Speed.FULL10);
                    case "2" -> portInfoItem.setSpeed(PortInfo.Speed.HALF10);
                }
            }

            ports.add(portInfoItem);
        }

        List<FdbItem> fdbTable = new ArrayList<>();
        Pattern fdbInfoPattern = Pattern.compile("\\[(?<portName>\\d{1,2}),'(?<mac>[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2}-[A-F\\d]{2})',(?<vid>\\d{1,4}),(?<dynamic>\\d)]");
        Matcher fdbInfoMatcher = fdbInfoPattern.matcher(fdbInfo);
        while (fdbInfoMatcher.find()) {
            FdbItem fdbItem = FdbItem.builder()
                    .portId(Integer.parseInt(fdbInfoMatcher.group("portName")))
                    .mac(fdbInfoMatcher.group("mac").toLowerCase().replaceAll("-", ":"))
                    .dynamic(fdbInfoMatcher.group("dynamic").equals("1"))
                    .vid(Integer.parseInt(fdbInfoMatcher.group("vid")))
                    .vlanName("Нет имени")
                    .build();
            fdbTable.add(fdbItem);
        }

        fdbTable.sort(Comparator.comparingInt(FdbItem::getPortId));
        for (FdbItem item : fdbTable) {
            ports.stream().filter(port -> Objects.equals(port.getPortId(), item.getPortId())).findFirst().ifPresent(port -> port.appendToMacTable(item));
        }

        return ports;
    }

    public static Integer convertUptime(String uptime) {
        Pattern uptimeCalc = Pattern.compile("((?<day>\\d{1,4}) days,? )?((?<hour>\\d{1,2}) (hrs|hours),? )?((?<min>\\d{1,2}) (min|mins|minutes),? )?((?<sec>\\d{1,2}) (secs|seconds))");
        Matcher uptimeCalcMatcher = uptimeCalc.matcher(uptime);

        if (!uptimeCalcMatcher.find())
            throw new ParsingException("Не удалось вычислить uptime системы");

        int days = 0, hours = 0, minutes = 0, seconds = 0;
        try {
            days = Integer.parseInt(uptimeCalcMatcher.group("day"));
        } catch (IllegalArgumentException ignore) {
        }
        try {
            hours = Integer.parseInt(uptimeCalcMatcher.group("hour"));
        } catch (IllegalArgumentException ignore) {
        }
        try {
            minutes = Integer.parseInt(uptimeCalcMatcher.group("min"));
        } catch (IllegalArgumentException ignore) {
        }
        try {
            seconds = Integer.parseInt(uptimeCalcMatcher.group("sec"));
        } catch (IllegalArgumentException ignore) {
        }
        seconds += minutes * 60;
        seconds += hours * 3600;
        seconds += days * 86400;

        return seconds;
    }

    public static Integer convertUptime(List<String> uptime) {
        int days, hours, minutes, seconds;
        days = Integer.parseInt(uptime.get(0));
        hours = Integer.parseInt(uptime.get(1));
        minutes = Integer.parseInt(uptime.get(2));
        seconds = Integer.parseInt(uptime.get(3));

        seconds += minutes * 60;
        seconds += hours * 3600;
        seconds += days * 86400;

        return seconds;
    }
}
