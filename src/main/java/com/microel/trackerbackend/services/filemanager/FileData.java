package com.microel.trackerbackend.services.filemanager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileData {
    private String name;
    private Long modified;
    private byte[] data;
    private String type;
}
