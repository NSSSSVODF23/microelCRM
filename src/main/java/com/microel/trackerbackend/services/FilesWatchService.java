package com.microel.trackerbackend.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Executors;

//@Service
public class FilesWatchService {
    WatchService watchService = FileSystems.getDefault().newWatchService();
    Path watchPath = Paths.get("./files");
    public FilesWatchService() throws IOException {
        Executors.newSingleThreadExecutor().execute(()->{
            try {
                watchPath.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                );

                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        System.out.println(
                                "Event kind:" + event.kind()
                                        + ". File affected: " + event.context() + ".");
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
