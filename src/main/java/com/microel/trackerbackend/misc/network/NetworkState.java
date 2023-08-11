package com.microel.trackerbackend.misc.network;

public enum NetworkState {
    OFFLINE("OFFLINE"),
    ONLINE("ONLINE");

    private String state;

    NetworkState(String state){
        this.state = state;
    }
}
