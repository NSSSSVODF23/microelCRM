package com.microel.trackerbackend.parsers.oldtracker;

import com.microel.confstore.AbstractConfiguration;

import java.util.HashMap;
import java.util.UUID;

public class AddressCorrectingPool extends HashMap<UUID, OldTracker.AddressCorrecting> implements AbstractConfiguration {

    @Override
    public String fileName() {
        return "addressCorrectingPool.json";
    }

    @Override
    public Boolean isFilled() {
        return true;
    }
}
