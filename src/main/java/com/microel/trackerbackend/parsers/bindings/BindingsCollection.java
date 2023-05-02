package com.microel.trackerbackend.parsers.bindings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BindingsCollection {
    private AccidentBindings accident;
    private ConnectionBindings connection;
    private PrivateSectorVD privateSectorVD;
    private PrivateSectorRM privateSectorRM;
}
