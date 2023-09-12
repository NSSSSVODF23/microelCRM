package com.microel.trackerbackend.parsers.commutator.parsers;

import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.storage.entities.acp.commutator.FdbItem;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//                builder.macTable(new ArrayList<>());
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
//            ports.stream().filter(port -> Objects.equals(port.getPortId(), item.getPortId())).findFirst().ifPresent(port -> port.getMacTable().add(item));
        }

        return ports;
    }

    public static Integer convertUptime(String uptime) {
        Pattern uptimeCalc = Pattern.compile("((?<day>\\d{1,4}) days, )?((?<hour>\\d{1,2}) (hrs|hours), )?((?<min>\\d{1,2}) (min|minutes), )?((?<sec>\\d{1,2}) (secs|seconds))");
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
}
