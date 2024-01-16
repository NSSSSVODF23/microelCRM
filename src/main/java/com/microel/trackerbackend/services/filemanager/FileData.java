package com.microel.trackerbackend.services.filemanager;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Объект хранящий в себе информацию о файле, его имя, mime тип, массив байт данных и дату последнего сохранения
 */
@Getter
@Setter
public class FileData {
    private String name;
    private Long modified;
    private byte[] data;
    private String type;

    public static FileData of(String name, String mime, byte[] fileBytes) {
        FileData fileData = new FileData();
        fileData.setName(name);
        fileData.setType(mime);
        fileData.setData(fileBytes);
        fileData.setModified(Instant.now().toEpochMilli());
        return fileData;
    }
}
