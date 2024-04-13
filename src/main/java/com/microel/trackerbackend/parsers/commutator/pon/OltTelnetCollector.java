package com.microel.trackerbackend.parsers.commutator.pon;

import com.microel.tdo.pon.MacTableEntry;
import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.util.List;

public interface OltTelnetCollector {
    void setLogin(String login);
    void setPassword(String password);
    void auth();

    List<MacTableEntry> getOntMacTable();

    void close();
}
