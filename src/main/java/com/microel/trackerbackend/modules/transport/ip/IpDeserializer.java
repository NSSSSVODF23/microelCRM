package com.microel.trackerbackend.modules.transport.ip;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class IpDeserializer extends StdDeserializer<IP> {

    public IpDeserializer() {
        this(null);
    }

    protected IpDeserializer(Class<IP> vc) {
        super(vc);
    }

    @Override
    public IP deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        try {
            return new IP(p.getValueAsString());
        } catch (Exception e) {
            return null;
        }
    }
}
