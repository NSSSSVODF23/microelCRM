package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.TelnetParser;
import com.microel.trackerbackend.parsers.commutator.exceptions.AuthorizationException;
import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.parsers.commutator.parsers.HuaweiParser;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.util.List;
import java.util.regex.Pattern;

public class HuaweiOldRemoteAccess extends CommutatorCredentials implements AbstractRemoteAccess {

    private TelnetParser telnetParser = new TelnetParser();

    public HuaweiOldRemoteAccess(String ip, String login, String password) {
        super(ip, login, password);
    }

    public HuaweiOldRemoteAccess(String ip) {
        super(ip);
    }

    @Override
    public void auth() {
        telnetParser.connect(getIp());
        telnetParser.listen("Login authentication");
        telnetParser.sendCommand(getLogin());
        String loginResponse = telnetParser.sendCommand(getPassword());
        String[] errorPatterns = new String[]{"Error"};
        for (String errorPattern : errorPatterns) {
            Pattern pattern = Pattern.compile(errorPattern);
            if (pattern.matcher(loginResponse).find()) {
                throw new AuthorizationException("Неверный логин или пароль " + getIp());
            }
        }
        telnetParser.sendCommand("screen-length 0 temporary");
    }

    @Override
    public SystemInfo getSystemInfo() {
        String data = telnetParser.sendCommand("display version");
        if(data.contains("Quidway S3928TP-SI")) throw new ParsingException("Не верная модель Huawei");
        return HuaweiParser.parseSiOldHu(data);
    }

    @Override
    public List<PortInfo> getPorts() {
        String portsData = telnetParser.sendCommand("display interface", "<[^>]+>");
        String fdbData = telnetParser.sendCommand("display mac-address", "<[^>]+>");
        return HuaweiParser.parsePortsOldHu(portsData, fdbData);
    }

    @Override
    public void close() {
        telnetParser.close();
    }
}
