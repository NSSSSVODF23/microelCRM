package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.TelnetParser;
import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.parsers.commutator.parsers.DlinkParser;
import com.microel.trackerbackend.parsers.commutator.parsers.HuaweiParser;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.sf.expectit.matcher.Matchers.contains;
import static net.sf.expectit.matcher.Matchers.regexp;

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
        try {
            telnetParser.sendCommand(getLogin()).expect(contains(":"));
            telnetParser.sendCommand(getPassword()).expect(regexp("<[^>]+>"));
            telnetParser.sendCommand("screen-length 0 temporary").expect(regexp("<[^>]+>"));
        } catch (IOException e) {
            close();
            throw new ParsingException("Не удалось авторизоваться на устройстве");
        }
    }

    @Override
    public SystemInfo getSystemInfo() {
        try {
            String data = telnetParser.sendCommand("display version").expect(regexp("<[^>]+>")).getBefore();
            if (data.contains("Quidway S3928TP-SI")) throw new ParsingException("Не верная модель Huawei");
            return HuaweiParser.parseSiOldHu(data);
        } catch (IOException e) {
            close();
            throw new ParsingException("Не удалось получить информацию о системе");
        }
    }

    @Override
    public List<PortInfo> getPorts() {
        try {
            String portsData = telnetParser.sendCommand("display interface").expect(regexp("<[^>]+>")).getBefore();
            String fdbData = telnetParser.sendCommand("display mac-address").expect(regexp("<[^>]+>")).getBefore();
            return HuaweiParser.parsePortsOldHu(portsData, fdbData);
        } catch (IOException e) {
            close();
            throw new ParsingException("Не удалось получить информацию о портах");
        }
    }

    @Override
    public void close() {
        telnetParser.close();
    }
}
