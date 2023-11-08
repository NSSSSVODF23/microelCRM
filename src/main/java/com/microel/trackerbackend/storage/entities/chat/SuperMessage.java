package com.microel.trackerbackend.storage.entities.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.dto.comment.AttachmentDto;
import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.AttachmentType;
import com.microel.trackerbackend.storage.entities.team.Employee;
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
        Set<AttachmentType> attachmentTypeSet = attachments.stream().map(AttachmentDto::getType).collect(Collectors.toSet());
        boolean isVisual = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.PHOTO || t == AttachmentType.VIDEO);
        boolean isAudio = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.AUDIO);
        boolean isDocument = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.DOCUMENT || t == AttachmentType.FILE);
        if(isVisual) return ContentType.VISUAL;
        if(isAudio) return ContentType.AUDIO;
        if(isDocument) return ContentType.FILE;
        return null;
    }

    @Nullable
    public String getDescriptionOfAttachment(){
        if(attachments == null || attachments.isEmpty()) return null;
        Set<AttachmentType> attachmentTypeSet = attachments.stream().map(AttachmentDto::getType).collect(Collectors.toSet());

        boolean isPhoto = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.PHOTO);
        boolean isVideo = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.VIDEO);
        boolean isVisual = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.PHOTO || t == AttachmentType.VIDEO);
        boolean isAudio = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.AUDIO);
        boolean isDocument = attachmentTypeSet.stream().allMatch(t->t == AttachmentType.DOCUMENT || t == AttachmentType.FILE);

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
