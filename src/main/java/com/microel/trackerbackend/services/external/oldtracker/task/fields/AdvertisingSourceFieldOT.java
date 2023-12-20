package com.microel.trackerbackend.services.external.oldtracker.task.fields;

import com.microel.trackerbackend.services.external.oldtracker.task.TaskFieldOT;
import com.microel.trackerbackend.storage.entities.templating.AdvertisingSource;
import lombok.Data;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;


@Data
public class AdvertisingSourceFieldOT implements TaskFieldOT {
    @NonNull
    private Integer id;
    @NonNull
    private String name;
//    private Map<AdvertisingSource, Integer> values = new HashMap<AdvertisingSource, Integer>() {{
//        put(AdvertisingSource.RESUMPTION, 0);
//        put(AdvertisingSource.LOSS, 1);
//        put(AdvertisingSource.MAIL, 2);
//        put(AdvertisingSource.LEAFLET, 3);
//        put(AdvertisingSource.SOUND, 4);
//        put(AdvertisingSource.RADIO, 5);
//        put(AdvertisingSource.SOCIALNET, 6);
//        put(AdvertisingSource.BANNER, 7);
//        put(AdvertisingSource.KITH, 8);
//        put(AdvertisingSource.SMS, 9);
//        put(AdvertisingSource.INTERNET, 10);
//        put(AdvertisingSource.MANAGER, 11);
//        put(AdvertisingSource.EARLYUSED, 12);
//    }};
    private Type type = Type.AD_SOURCE;
}
