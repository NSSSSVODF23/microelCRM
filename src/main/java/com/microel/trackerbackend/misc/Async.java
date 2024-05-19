package com.microel.trackerbackend.misc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Async {
    public static void of(Runnable r){
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(r);
        executorService.shutdown();
    }
}
