package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.TelnetParser;
import com.microel.trackerbackend.parsers.commutator.exceptions.AuthorizationException;
import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import com.microel.trackerbackend.parsers.commutator.parsers.DlinkParser;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static net.sf.expectit.matcher.Matchers.contains;

public class DES18RemoteAccess extends CommutatorCredentials implements AbstractRemoteAccess {

    private TelnetParser telnetParser = new TelnetParser();

    public DES18RemoteAccess(String ip, String login, String password) {
        super(ip, login, password);
    }

    public DES18RemoteAccess(String ip) {
        super(ip);
    }

    @Override
    public void auth() {
        telnetParser.connect(getIp());
        telnetParser.listen("DES-3200-18");
        try {
            telnetParser.sendCommand(getLogin()).expect(contains(":"));
            telnetParser.sendCommand(getPassword()).expect(contains("#"));
        } catch (IOException e) {
            throw new ParsingException("Не удалось авторизоваться на устройстве");
        }
    }

    @Override
    public SystemInfo getSystemInfo() {
        try {
            telnetParser.sendCommand("disable clipaging").expect(contains("#"));
            String data = telnetParser.sendCommand("show switch").expect(contains("#")).getBefore();
            telnetParser.sendCommand("enable clipaging").expect(contains("#"));
            return DlinkParser.parseSIDes28(data);
        } catch (IOException e) {
            throw new ParsingException("Не удалось получить информацию о системе");
        }
    }

    @Override
    public List<PortInfo> getPorts() {
        try {
            telnetParser.sendCommand("disable clipaging").expect(contains("#"));

            List<String> rawPorts = new ArrayList<>();
            for (int i = 1; i <= 18 ; i++) {
                rawPorts.add(telnetParser.sendCommand("show ports " + i).expect(contains("#")).getBefore());
            }

            List<String> rawFdb = new ArrayList<>();
            for (int i = 1; i <= 18 ; i++) {
                rawFdb.add(telnetParser.sendCommand("show fdb port " + i).expect(contains("#")).getBefore());
            }

            telnetParser.sendCommand("enable clipaging").expect(contains("#"));
            if(rawPorts.size() != 18) throw new ParsingException("Не удалось получить верное кол-во портов");
            return DlinkParser.parsePortsDes28(rawPorts, rawFdb);
        } catch (IOException e) {
            throw new ParsingException("Не удалось получить информацию о портах");
        }
    }

    @Override
    public void close() {
        telnetParser.close();
    }
}
