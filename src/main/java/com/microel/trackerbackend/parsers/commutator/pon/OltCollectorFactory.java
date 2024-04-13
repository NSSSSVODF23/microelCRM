package com.microel.trackerbackend.parsers.commutator.pon;

import com.microel.tdo.pon.OltModel;
import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.ra.DES18RemoteAccess;
import com.microel.trackerbackend.parsers.commutator.ra.DES28RemoteAccess;
import com.microel.trackerbackend.parsers.commutator.ra.HuaweiOldRemoteAccess;
import com.microel.trackerbackend.parsers.commutator.ra.HuaweiRemoteAccess;
import com.microel.trackerbackend.services.api.ResponseException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OltCollectorFactory {
    private final Map<OltModel, Class<? extends OltTelnetCollector>> modelMap = new HashMap<>();

    public OltCollectorFactory() {
        modelMap.put(OltModel.BdcomP3608, BdcomTelnet.class);
        modelMap.put(OltModel.CDataFD1104SN, Cdata1104Telnet.class);
        modelMap.put(OltModel.CDataFD1204S, Cdata1204Telnet.class);
        modelMap.put(OltModel.CDataFD1208S, Cdata1208Telnet.class);
        modelMap.put(OltModel.CDataFD1216S, Cdata1216Telnet.class);
    }

    public OltTelnetCollector getOltCollector(OltModel model, String ip) {
        try {
            Class<? extends OltTelnetCollector> remoteAccessClass = modelMap.get(model);
            if(remoteAccessClass == null) throw new ResponseException("Не установлена конфигурация доступа для модели: " + model);
            return remoteAccessClass.getConstructor(String.class).newInstance(ip);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ResponseException(e.getMessage());
        }
    }
}
