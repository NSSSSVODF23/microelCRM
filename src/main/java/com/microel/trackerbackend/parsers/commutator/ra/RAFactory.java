package com.microel.trackerbackend.parsers.commutator.ra;

import com.microel.trackerbackend.parsers.commutator.AbstractRemoteAccess;
import com.microel.trackerbackend.services.api.ResponseException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Service
public class RAFactory {

    private Map<String, Class<? extends AbstractRemoteAccess>> modelMap = new HashMap<>();

    public RAFactory() {
        modelMap.put("DES-1210-28/ME", DES28RemoteAccess.class);
        modelMap.put("DES-3028", DES28RemoteAccess.class);
        modelMap.put("DES-1228/ME", DES28RemoteAccess.class);
        modelMap.put("DES-1100-16", DES16WebAccess.class);
        modelMap.put("DES-1100-24", DES16WebAccess.class);
        modelMap.put("DGS-1100-8", DGS8WebAccess.class);
        modelMap.put("DGS-1100-5", DGS8WebAccess.class);
    }

    public AbstractRemoteAccess getRemoteAccess(String model, String ip) {
        try {
            Class<? extends AbstractRemoteAccess> remoteAccessClass = modelMap.get(model);
            if(remoteAccessClass == null) throw new ResponseException("Не установлена конфигурация доступа для модели: " + model);
            return remoteAccessClass.getConstructor(String.class).newInstance(ip);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ResponseException(e.getMessage());
        }
    }
}
