package com.microel.trackerbackend.controllers.telegram;

import com.microel.trackerbackend.storage.entities.comments.AttachmentType;
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
            {"image/jpeg", new Type(AttachmentType.PHOTO, "jpg")},
            {"image/gif", new Type(AttachmentType.PHOTO, "gif")},
            {"image/png", new Type(AttachmentType.PHOTO, "png")},
            {"application/pdf", new Type(AttachmentType.DOCUMENT, "pdf")},
            {"audio/mpeg", new Type(AttachmentType.AUDIO, "mp3")},
            {"video/quicktime", new Type(AttachmentType.VIDEO, "mov")},
            {"video/mp4", new Type(AttachmentType.VIDEO, "mp4")},
            {"video/webp", new Type(AttachmentType.PHOTO, "webp")},
            {"audio/aac", new Type(AttachmentType.AUDIO, "aac")},
            {"application/x-abiword", new Type(AttachmentType.DOCUMENT, "abw")},
            {"application/x-freearc", new Type(AttachmentType.DOCUMENT, "arc")},
            {"image/avif", new Type(AttachmentType.PHOTO, "avif")},
            {"video/x-msvideo", new Type(AttachmentType.VIDEO, "avi")},
            {"application/vnd.amazon.ebook", new Type(AttachmentType.DOCUMENT, "azw")},
            {"application/octet-stream", new Type(AttachmentType.FILE, "bin")},
            {"image/bmp", new Type(AttachmentType.PHOTO, "bmp")},
            {"application/x-bzip", new Type(AttachmentType.FILE, "bz")},
            {"application/x-bzip2", new Type(AttachmentType.FILE, "bz2")},
            {"application/x-cdf", new Type(AttachmentType.FILE, "cda")},
            {"application/x-csh", new Type(AttachmentType.FILE, "csh")},
            {"text/css", new Type(AttachmentType.DOCUMENT, "css")},
            {"text/csv", new Type(AttachmentType.DOCUMENT, "csv")},
            {"application/msword", new Type(AttachmentType.FILE, "doc")},
            {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", new Type(AttachmentType.FILE, "docx")},
            {"application/vnd.ms-fontobject", new Type(AttachmentType.FILE, "eot")},
            {"application/epub+zip", new Type(AttachmentType.FILE, "epub")},
            {"application/gzip", new Type(AttachmentType.FILE, "gz")},
            {"image/gif", new Type(AttachmentType.PHOTO, "gif")},
            {"text/html", new Type(AttachmentType.DOCUMENT, "html")},
            {"image/vnd.microsoft.icon", new Type(AttachmentType.PHOTO, "ico")},
            {"text/calendar", new Type(AttachmentType.FILE, "ics")},
            {"application/java-archive", new Type(AttachmentType.FILE, "jar")},
            {"text/javascript", new Type(AttachmentType.DOCUMENT, "js")},
            {"application/json", new Type(AttachmentType.DOCUMENT, "json")},
            {"application/ld+json", new Type(AttachmentType.DOCUMENT, "jsonld")},
            {"audio/midi", new Type(AttachmentType.AUDIO, "midi")},
            {"audio/x-midi", new Type(AttachmentType.AUDIO, "midi")},
            {"application/vnd.apple.installer+xml", new Type(AttachmentType.FILE, "mpkg")},
            {"application/vnd.oasis.opendocument.presentation", new Type(AttachmentType.FILE, "odp")},
            {"application/vnd.oasis.opendocument.spreadsheet", new Type(AttachmentType.FILE, "ods")},
            {"application/vnd.oasis.opendocument.text", new Type(AttachmentType.FILE, "odt")},
            {"audio/ogg", new Type(AttachmentType.AUDIO, "oga")},
            {"video/ogg", new Type(AttachmentType.VIDEO, "ogv")},
            {"application/ogg", new Type(AttachmentType.FILE, "ogx")},
            {"audio/opus", new Type(AttachmentType.AUDIO, "opus")},
            {"font/otf", new Type(AttachmentType.FILE, "otf")},
            {"application/x-httpd-php", new Type(AttachmentType.FILE, "php")},
            {"application/vnd.ms-powerpoint", new Type(AttachmentType.FILE, "ppt")},
            {"application/vnd.openxmlformats-officedocument.presentationml.presentation", new Type(AttachmentType.FILE, "pptx")},
            {"application/vnd.rar", new Type(AttachmentType.FILE, "rar")},
            {"application/rtf", new Type(AttachmentType.FILE, "rtf")},
            {"application/x-sh", new Type(AttachmentType.FILE, "sh")},
            {"image/svg+xml", new Type(AttachmentType.PHOTO, "svg")},
            {"application/x-tar", new Type(AttachmentType.FILE, "tar")},
            {"image/tiff", new Type(AttachmentType.PHOTO, "tiff")},
            {"video/mp2t", new Type(AttachmentType.VIDEO, "ts")},
            {"font/ttf", new Type(AttachmentType.FILE, "ttf")},
            {"text/plain", new Type(AttachmentType.DOCUMENT, "txt")},
            {"application/vnd.visio", new Type(AttachmentType.FILE, "vsd")},
            {"audio/wav", new Type(AttachmentType.AUDIO, "wav")},
            {"audio/webm", new Type(AttachmentType.AUDIO, "weba")},
            {"video/webm", new Type(AttachmentType.VIDEO, "webm")},
            {"image/webp", new Type(AttachmentType.PHOTO, "webp")},
            {"font/woff", new Type(AttachmentType.FILE, "woff")},
            {"font/woff2", new Type(AttachmentType.FILE, "woff2")},
            {"application/xhtml+xml", new Type(AttachmentType.DOCUMENT, "xhtml")},
            {"application/vnd.ms-excel", new Type(AttachmentType.FILE, "xls")},
            {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new Type(AttachmentType.FILE, "xlsx")},
            {"application/xml", new Type(AttachmentType.DOCUMENT, "xml")},
            {"application/vnd.mozilla.xul+xml", new Type(AttachmentType.FILE, "xul")},
            {"application/zip", new Type(AttachmentType.FILE, "zip")},
            {"video/3gpp", new Type(AttachmentType.VIDEO, "3gp")},
            {"audio/3gpp",  new Type(AttachmentType.VIDEO, "3gp")},
            {"video/3gpp2", new Type(AttachmentType.VIDEO, "3g2")},
            {"audio/3gpp2", new Type(AttachmentType.VIDEO, "3g2")},
            {"application/x-7z-compressed", new Type(AttachmentType.FILE, "7z")}
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
        private AttachmentType type;
        private String extension;
    }

}
