package com.microel.trackerbackend.controllers.configuration.entity;

import com.microel.trackerbackend.controllers.configuration.AbstractConfiguration;

import java.util.ArrayList;

public class DefaultCitiesConf extends ArrayList<DefaultCitiesConf.CityDef> implements AbstractConfiguration {
    public static class CityDef{
        public String name;
        public ArrayList<String> streets;
    }

    @Override
    public Boolean isFilled() {
        return true;
    }
}
