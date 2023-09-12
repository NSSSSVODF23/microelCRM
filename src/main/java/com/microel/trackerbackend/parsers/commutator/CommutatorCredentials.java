package com.microel.trackerbackend.parsers.commutator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class CommutatorCredentials {
    private String ip;
    private String login = "admin";
    private String password = "gjkjcfnsq";

    public CommutatorCredentials(String ip) {
        this.ip = ip;
    }
}
