package com.microel.trackerbackend.parsers.commutator;

import com.microel.trackerbackend.storage.entities.acp.commutator.PortInfo;
import com.microel.trackerbackend.storage.entities.acp.commutator.SystemInfo;

import java.util.List;

public interface AbstractRemoteAccess {
    void setLogin(String login);
    void setPassword(String password);
    void auth();

    SystemInfo getSystemInfo();

    List<PortInfo> getPorts();

    void close();
}
