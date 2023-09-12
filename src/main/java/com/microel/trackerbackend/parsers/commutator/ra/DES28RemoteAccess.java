package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.TelnetParser;
import com.microel.trackerbackend.parsers.commutator.exceptions.AuthorizationException;
import com.microel.trackerbackend.parsers.commutator.parsers.DlinkParser;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.util.List;
import java.util.regex.Pattern;

public class DES28RemoteAccess extends CommutatorCredentials implements AbstractRemoteAccess {

    private TelnetParser telnetParser = new TelnetParser();

    public DES28RemoteAccess(String ip, String login, String password) {
        super(ip, login, password);
    }

    public DES28RemoteAccess(String ip) {
        super(ip);
    }

    @Override
    public void auth() {
        telnetParser.connect(getIp());
        telnetParser.listen("DES-1210-28/ME", "DES-3028", "DES-1228/ME");
        telnetParser.sendCommand(getLogin());
        String loginResponse = telnetParser.sendCommand(getPassword());
        String[] errorPatterns = new String[]{"Fail!", "Incorrect"};
        for (String errorPattern : errorPatterns) {
            Pattern pattern = Pattern.compile(errorPattern);
            if (pattern.matcher(loginResponse).find()) {
                throw new AuthorizationException("Неверный логин или пароль " + getIp());
            }
        }
    }

    @Override
    public SystemInfo getSystemInfo() {
        telnetParser.sendCommand("disable clipaging");
        String data = telnetParser.sendCommand("show switch");
        telnetParser.sendCommand("enable clipaging");
        telnetParser.close();
        return DlinkParser.parseSIDes28(data);
    }

    @Override
    public List<PortInfo> getPorts() {
        telnetParser.sendCommand("disable clipaging");
        String portsData = "";
        if(telnetParser.isHardwareVariant("DES-1210-28/ME")) {
            portsData += telnetParser.sendCommand("show ports description").replace("\u001B[?25l", "");
            for (int i = 0; i < 4; i++) {
                portsData += telnetParser.send(" ");
            }
            telnetParser.send("q");
        }else{
            portsData = telnetParser.sendCommand("show ports description");
        }
        String portsTypeData = telnetParser.sendCommand("show ports media_type", "#");
        String fdb = telnetParser.sendCommand("show fdb", "#");
        telnetParser.sendCommand("enable clipaging");
        telnetParser.close();
        return DlinkParser.parsePortsDes28(portsData, portsTypeData, fdb);
    }

    @Override
    public void close() {
        telnetParser.close();
    }
}
