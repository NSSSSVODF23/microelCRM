package com.microel.trackerbackend.misc.autosupport.schema;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class AutoSupportStorage {
    private final Map<UUID, Map<String, String>> preprocessResults = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> predicateResults = new ConcurrentHashMap<>();
    private final Map<UUID, String> inputResults = new ConcurrentHashMap<>();

    public Map<String, String> getPredicateArgsByMap(Map<String, String> argsMap) {
        final Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : argsMap.entrySet()){
            final String argName = entry.getKey();
            final String token = entry.getValue();
            final String[] subTokens = token.split(":");
            if(subTokens.length == 2) {
                final String tokenType = subTokens[0];
                final UUID inputNodeId = UUID.fromString(subTokens[1]);
                String value = getInputResults().get(inputNodeId);
                result.put(argName, value);
            }else if (subTokens.length == 3) {
                final String tokenType = subTokens[0];
                final UUID preprocessNodeId = UUID.fromString(subTokens[1]);
                final String preprocessNodeKey = subTokens[2];
                String value = getPreprocessResults().get(preprocessNodeId).get(preprocessNodeKey);
                result.put(argName, value);
            }
        }
        return result;
    }
}
