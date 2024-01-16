package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.storage.entities.comments.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.games.Animation;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MimeConvertor {
    private static final Map<String, Type> mimeMap = Stream.of(new Object[][]{
            {"image/jpeg", new Type(FileType.PHOTO, "jpg")},
            {"image/gif", new Type(FileType.PHOTO, "gif")},
            {"image/png", new Type(FileType.PHOTO, "png")},
            {"application/pdf", new Type(FileType.DOCUMENT, "pdf")},
            {"audio/mpeg", new Type(FileType.AUDIO, "mp3")},
            {"video/quicktime", new Type(FileType.VIDEO, "mov")},
            {"video/mp4", new Type(FileType.VIDEO, "mp4")},
            {"video/webp", new Type(FileType.PHOTO, "webp")},
            {"audio/aac", new Type(FileType.AUDIO, "aac")},
            {"application/x-abiword", new Type(FileType.DOCUMENT, "abw")},
            {"application/x-freearc", new Type(FileType.DOCUMENT, "arc")},
            {"image/avif", new Type(FileType.PHOTO, "avif")},
            {"video/x-msvideo", new Type(FileType.VIDEO, "avi")},
            {"application/vnd.amazon.ebook", new Type(FileType.DOCUMENT, "azw")},
            {"application/octet-stream", new Type(FileType.FILE, "bin")},
            {"image/bmp", new Type(FileType.PHOTO, "bmp")},
            {"application/x-bzip", new Type(FileType.FILE, "bz")},
            {"application/x-bzip2", new Type(FileType.FILE, "bz2")},
            {"application/x-cdf", new Type(FileType.FILE, "cda")},
            {"application/x-csh", new Type(FileType.FILE, "csh")},
            {"text/css", new Type(FileType.DOCUMENT, "css")},
            {"text/csv", new Type(FileType.DOCUMENT, "csv")},
            {"application/msword", new Type(FileType.FILE, "doc")},
            {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", new Type(FileType.FILE, "docx")},
            {"application/vnd.ms-fontobject", new Type(FileType.FILE, "eot")},
            {"application/epub+zip", new Type(FileType.FILE, "epub")},
            {"application/gzip", new Type(FileType.FILE, "gz")},
            {"image/gif", new Type(FileType.PHOTO, "gif")},
            {"text/html", new Type(FileType.DOCUMENT, "html")},
            {"image/vnd.microsoft.icon", new Type(FileType.PHOTO, "ico")},
            {"text/calendar", new Type(FileType.FILE, "ics")},
            {"application/java-archive", new Type(FileType.FILE, "jar")},
            {"text/javascript", new Type(FileType.DOCUMENT, "js")},
            {"application/json", new Type(FileType.DOCUMENT, "json")},
            {"application/ld+json", new Type(FileType.DOCUMENT, "jsonld")},
            {"audio/midi", new Type(FileType.AUDIO, "midi")},
            {"audio/x-midi", new Type(FileType.AUDIO, "midi")},
            {"application/vnd.apple.installer+xml", new Type(FileType.FILE, "mpkg")},
            {"application/vnd.oasis.opendocument.presentation", new Type(FileType.FILE, "odp")},
            {"application/vnd.oasis.opendocument.spreadsheet", new Type(FileType.FILE, "ods")},
            {"application/vnd.oasis.opendocument.text", new Type(FileType.FILE, "odt")},
            {"audio/ogg", new Type(FileType.AUDIO, "oga")},
            {"video/ogg", new Type(FileType.VIDEO, "ogv")},
            {"application/ogg", new Type(FileType.FILE, "ogx")},
            {"audio/opus", new Type(FileType.AUDIO, "opus")},
            {"font/otf", new Type(FileType.FILE, "otf")},
            {"application/x-httpd-php", new Type(FileType.FILE, "php")},
            {"application/vnd.ms-powerpoint", new Type(FileType.FILE, "ppt")},
            {"application/vnd.openxmlformats-officedocument.presentationml.presentation", new Type(FileType.FILE, "pptx")},
            {"application/vnd.rar", new Type(FileType.FILE, "rar")},
            {"application/rtf", new Type(FileType.FILE, "rtf")},
            {"application/x-sh", new Type(FileType.FILE, "sh")},
            {"image/svg+xml", new Type(FileType.PHOTO, "svg")},
            {"application/x-tar", new Type(FileType.FILE, "tar")},
            {"image/tiff", new Type(FileType.PHOTO, "tiff")},
            {"video/mp2t", new Type(FileType.VIDEO, "ts")},
            {"font/ttf", new Type(FileType.FILE, "ttf")},
            {"text/plain", new Type(FileType.DOCUMENT, "txt")},
            {"application/vnd.visio", new Type(FileType.FILE, "vsd")},
            {"audio/wav", new Type(FileType.AUDIO, "wav")},
            {"audio/webm", new Type(FileType.AUDIO, "weba")},
            {"video/webm", new Type(FileType.VIDEO, "webm")},
            {"image/webp", new Type(FileType.PHOTO, "webp")},
            {"font/woff", new Type(FileType.FILE, "woff")},
            {"font/woff2", new Type(FileType.FILE, "woff2")},
            {"application/xhtml+xml", new Type(FileType.DOCUMENT, "xhtml")},
            {"application/vnd.ms-excel", new Type(FileType.FILE, "xls")},
            {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new Type(FileType.FILE, "xlsx")},
            {"application/xml", new Type(FileType.DOCUMENT, "xml")},
            {"application/vnd.mozilla.xul+xml", new Type(FileType.FILE, "xul")},
            {"application/zip", new Type(FileType.FILE, "zip")},
            {"video/3gpp", new Type(FileType.VIDEO, "3gp")},
            {"audio/3gpp",  new Type(FileType.VIDEO, "3gp")},
            {"video/3gpp2", new Type(FileType.VIDEO, "3g2")},
            {"audio/3gpp2", new Type(FileType.VIDEO, "3g2")},
            {"application/x-7z-compressed", new Type(FileType.FILE, "7z")}
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Type) data[1]));

    @Nullable
    public static Type from(Animation animation) {
        return mimeMap.get(animation.getMimetype());
    }

    @Nullable
    public static Type from(Audio audio) {
        return mimeMap.get(audio.getMimeType());
    }

    @Nullable
    public static Type from(Document document) {
        return mimeMap.get(document.getMimeType());
    }

    @Nullable
    public static Type from(Video video) {
        return mimeMap.get(video.getMimeType());
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Type {
        private FileType type;
        private String extension;
    }

}
