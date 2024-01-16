package com.microel.trackerbackend.storage.entities.chat;

import com.microel.trackerbackend.storage.dto.comment.AttachmentDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.comments.FileType;
import lombok.*;
import org.springframework.lang.Nullable;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SuperMessage {
    private Long superMessageId;
    private String text;
    private List<AttachmentDto> attachments;
    private Timestamp edited;
    private Boolean deleted;
    private EmployeeDto author;
    private SuperMessage replyTo;
    private Timestamp sendAt;
    private Set<EmployeeDto> readByEmployees;
    private Set<Long> includedMessages;
    private Long parentChatId;

    @Nullable
    public ContentType getAttachmentType(){
        Set<FileType> fileTypeSet = attachments.stream().map(AttachmentDto::getType).collect(Collectors.toSet());
        boolean isVisual = fileTypeSet.stream().allMatch(t->t == FileType.PHOTO || t == FileType.VIDEO);
        boolean isAudio = fileTypeSet.stream().allMatch(t->t == FileType.AUDIO);
        boolean isDocument = fileTypeSet.stream().allMatch(t->t == FileType.DOCUMENT || t == FileType.FILE);
        if(isVisual) return ContentType.VISUAL;
        if(isAudio) return ContentType.AUDIO;
        if(isDocument) return ContentType.FILE;
        return null;
    }

    @Nullable
    public String getDescriptionOfAttachment(){
        if(attachments == null || attachments.isEmpty()) return null;
        Set<FileType> fileTypeSet = attachments.stream().map(AttachmentDto::getType).collect(Collectors.toSet());

        boolean isPhoto = fileTypeSet.stream().allMatch(t->t == FileType.PHOTO);
        boolean isVideo = fileTypeSet.stream().allMatch(t->t == FileType.VIDEO);
        boolean isVisual = fileTypeSet.stream().allMatch(t->t == FileType.PHOTO || t == FileType.VIDEO);
        boolean isAudio = fileTypeSet.stream().allMatch(t->t == FileType.AUDIO);
        boolean isDocument = fileTypeSet.stream().allMatch(t->t == FileType.DOCUMENT || t == FileType.FILE);

        if(isPhoto) return attachments.size()+" фотографий";
        if(isVideo) return attachments.size()+" видео";
        if(isVisual) return attachments.size()+" медиафайлов";
        if(isAudio) return attachments.size()+" аудио";
        if(isDocument) return attachments.size()+" файлов";
        return null;
    }

    public boolean isMediaGroup(){
        return attachments != null && attachments.size() > 1;
    }

    public boolean isMultipleAttachments() {
        return attachments != null && attachments.size() > 1;
    }

    public boolean isHasAttachment() {
        return attachments != null && !attachments.isEmpty();
    }
}
