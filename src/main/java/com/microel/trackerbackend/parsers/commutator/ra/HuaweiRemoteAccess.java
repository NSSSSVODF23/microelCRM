package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.TelnetParser;
import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.parsers.commutator.parsers.HuaweiParser;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;
import net.sf.expectit.MultiResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static net.sf.expectit.matcher.Matchers.*;

public class HuaweiRemoteAccess extends CommutatorCredentials implements AbstractRemoteAccess {

    private final String[] interfacesList = {
            "Ethernet 1/0/1",
            "Ethernet 1/0/2",
            "Ethernet 1/0/3",
            "Ethernet 1/0/4",
            "Ethernet 1/0/5",
            "Ethernet 1/0/6",
            "Ethernet 1/0/7",
            "Ethernet 1/0/8",
            "Ethernet 1/0/9",
            "Ethernet 1/0/10",
            "Ethernet 1/0/11",
            "Ethernet 1/0/12",
            "Ethernet 1/0/13",
            "Ethernet 1/0/14",
            "Ethernet 1/0/15",
            "Ethernet 1/0/16",
            "Ethernet 1/0/17",
            "Ethernet 1/0/18",
            "Ethernet 1/0/19",
            "Ethernet 1/0/20",
            "Ethernet 1/0/21",
            "Ethernet 1/0/22",
            "Ethernet 1/0/23",
            "Ethernet 1/0/24",
            "GigabitEthernet 1/1/1",
            "GigabitEthernet 1/1/2",
            "GigabitEthernet 1/1/3",
            "GigabitEthernet 1/1/4"
    };
    private TelnetParser telnetParser = new TelnetParser();

    public HuaweiRemoteAccess(String ip, String login, String password) {
        super(ip, login, password);
    }

    public HuaweiRemoteAccess(String ip) {
        super(ip);
    }

    @Override
    public void auth() {
        telnetParser.connect(getIp());
        telnetParser.listen("Login authentication");
        try {
            telnetParser.sendCommand(getLogin()).expect(contains(":"));
            telnetParser.sendCommand(getPassword()).expect(regexp("<[^>]+>"));
//            telnetParser.sendCommand("screen-length 0 temporary").expect(regexp("<[^>]+>"));
        } catch (IOException e) {
            throw new ParsingException("Не удалось авторизоваться на устройстве");
        }
//        telnetParser.connect(getIp());
//        telnetParser.listen("Login authentication");
//        telnetParser.sendCommand(getLogin());
//        String loginResponse = telnetParser.sendCommand(getPassword());
//        String[] errorPatterns = new String[]{"Error"};
//        for (String errorPattern : errorPatterns) {
//            Pattern pattern = Pattern.compile(errorPattern);
//            if (pattern.matcher(loginResponse).find()) {
//                throw new AuthorizationException("Неверный логин или пароль " + getIp());
//            }
//        }
//        telnetParser.sendCommand("screen-length 0 temporary");
    }

    @Override
    public SystemInfo getSystemInfo() {
        try {
            String data = telnetParser.sendCommand("display version").expect(regexp("<[^>]+>")).getBefore();
            if (!data.contains("Quidway S3928TP-SI")) throw new ParsingException("Не верная модель Huawei");
            return HuaweiParser.parseSiHu(data);
        } catch (IOException e) {
            close();
            throw new ParsingException("Не удалось получить информацию о системе");
        }
    }

    @Override
    public List<PortInfo> getPorts() {
        List<String> portsData = new ArrayList<>();
        List<String> fdbData = new ArrayList<>();
        for (String interfaceName : interfacesList) {
            try {
                String data = telnetParser.sendCommand("display interface " + interfaceName).expect(contains("---- More ----")).getBefore();
                data += telnetParser.send(" ").expect(regexp("<[^>]+>")).getBefore();
                portsData.add(data);
            } catch (IOException e) {
                close();
                throw new ParsingException("Не удалось получить информацию о портах");
            }
        }
        for (String interfaceName : interfacesList) {
            try {
                MultiResult expect = telnetParser.sendCommand("display mac-address interface " + interfaceName).expect(anyOf(contains("---- More ----"), regexp("<[^>]+>")));
                String data = expect.getBefore();
                while (expect.getResults().get(0).isSuccessful()) {
                    expect = telnetParser.send(" ").expect(anyOf(contains("---- More ----"), regexp("<[^>]+>")));
                    data += expect.getBefore();
                }
                fdbData.add(data);
            } catch (IOException e) {
                close();
                throw new ParsingException("Не удалось получить информацию о mac-адресах");
            }
        }
        return HuaweiParser.parsePortsHu(portsData, fdbData);
    }

    @Override
    public void close() {
        telnetParser.close();
    }
}
