package com.microel.trackerbackend.modules.transport.ip;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.jackson.JsonObjectSerializer;

import java.io.IOException;

public class IpSerializer extends StdSerializer<IP> {

    public IpSerializer(){
        this(null);
    }

    public IpSerializer(Class<IP> t) {
        super(t);
    }

    @Override
    public void serialize(IP value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}
