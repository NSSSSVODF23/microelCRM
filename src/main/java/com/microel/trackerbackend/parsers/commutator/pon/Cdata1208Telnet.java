package com.microel.trackerbackend.parsers.commutator.pon;

import com.microel.tdo.pon.MacTableEntry;
import com.microel.trackerbackend.parsers.commutator.CommutatorCredentials;
import com.microel.trackerbackend.parsers.commutator.TelnetParser;
import com.microel.trackerbackend.parsers.commutator.exceptions.ParsingException;
import net.sf.expectit.MultiResult;
import net.sf.expectit.matcher.Matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static net.sf.expectit.matcher.Matchers.anyOf;
import static net.sf.expectit.matcher.Matchers.contains;

public class Cdata1208Telnet extends CommutatorCredentials implements OltTelnetCollector {

    private final TelnetParser telnetParser = new TelnetParser();

    public Cdata1208Telnet(String ip, String login, String password) {
        super(ip, login, password);
    }

    public Cdata1208Telnet(String ip) {
        super(ip);
    }

    @Override
    public void auth() {
        telnetParser.connect(getIp());
        telnetParser.listen("EPON OLT");
        try {
            telnetParser.sendCommand(getLogin()).expect(contains(":"));
            telnetParser.sendCommand(getPassword()).expect(contains(">"));
            telnetParser.sendCommand("enable").expect(contains("#"));
            telnetParser.sendCommand("config").expect(contains("#"));
        } catch (IOException e) {
            throw new ParsingException("Не удалось авторизоваться на устройстве");
        }
    }

    @Override
    public List<MacTableEntry> getOntMacTable() {
        List<MacTableEntry> macTable = new ArrayList<>();
        try {
            for (int i = 1; i <= 8; i++) {
                String command = "show mac-address port epon 0/0/" + i + " with-ont-location";
                Matcher<MultiResult> await = anyOf(contains("More"), contains("#"));

                MultiResult expect = telnetParser.sendCommand(command).expect(await);
                StringBuilder data = new StringBuilder(expect.getBefore());

                while (expect.getResults().get(0).isSuccessful()) {
                    expect = telnetParser.send(" ").expect(await);
                    data.append(expect.getBefore());
                }

                macTable.addAll(convertRawTable(data.toString()));
            }
        }catch (IOException ignore){}
        return macTable;
    }

    private List<MacTableEntry> convertRawTable(String rawTable) {
        List<MacTableEntry> macTable = new ArrayList<>();
        String[] lines = rawTable.split("\\r?\\n");
        Pattern pattern = Pattern.compile("(?<mac>[A-F\\d:]+)\\s+(?<vlan>\\d{1,4})\\s+pon\\d/\\d/(?<port>\\d{1,2})\\s+(?<pos>\\d{1,2})");
        for (String line : lines) {
            java.util.regex.Matcher matcher = pattern.matcher(line);
            if(matcher.find()){
                macTable.add(MacTableEntry.of(getIp(), matcher.group("mac"), matcher.group("vlan"), matcher.group("port"), matcher.group("pos")));
            }
        }
        return macTable;
    }

    @Override
    public void close() {
        telnetParser.close();
    }
}
