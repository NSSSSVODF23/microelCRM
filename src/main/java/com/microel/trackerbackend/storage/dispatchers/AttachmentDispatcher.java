package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.services.filemanager.FileSaver;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.AttachmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AttachmentDispatcher {
    private final AttachmentRepository attachmentRepository;
    private final FileSaver fileSaver;

    public AttachmentDispatcher(AttachmentRepository attachmentRepository, FileSaver fileSaver) {
        this.attachmentRepository = attachmentRepository;
        this.fileSaver = fileSaver;
    }

    public Attachment getAttachments(String id) throws EntryNotFound {
        Attachment attachment = this.attachmentRepository.findById(id).orElse(null);
        if(attachment == null) throw new EntryNotFound();
        return attachment;
    }

    public List<Attachment> saveAttachments(List<FileData> files) throws EmptyFile, WriteError {
        List<Attachment> result = new ArrayList<>();
        for (FileData file : files) {

            Timestamp modifiedTimestamp = Timestamp.from(Instant.ofEpochMilli(file.getModified()));

            Attachment.AttachmentBuilder attachBuilder = Attachment.builder()
                    .type(FileSaver.getAttachmentType(file))
                    .mimeType(file.getType())
                    .created(Timestamp.from(Instant.now()))
                    .modified(modifiedTimestamp)
                    .size((long) file.getData().length);

            Attachment foundAttachment = attachmentRepository.findById(file.getName()).orElse(null);

            if (foundAttachment != null) {
                if (foundAttachment.getModified().equals(modifiedTimestamp) && foundAttachment.getSize() == file.getData().length) {
                    result.add(foundAttachment);
                } else {
                    String newFileName = UUID.randomUUID() + "_" + file.getName();
                    file.setName(newFileName);
                    String path = fileSaver.save(file);
                    result.add(attachmentRepository.save(attachBuilder.name(newFileName).path(path).build()));
                }
            } else {
                String path = fileSaver.save(file);
                result.add(attachmentRepository.save(attachBuilder.name(file.getName()).path(path).build()));
            }

        }

        return result;
    }

    public List<Attachment> getByTask(Long taskId) {
        return attachmentRepository.findAll(
                (root, query, cb) ->
                        cb.and(cb.equal(root.join("comments").join("parent").get("taskId"), taskId)),
                Sort.by(Sort.Direction.DESC, "created"));
    }

    public Integer getCountByTask(Long taskId) {
        return getByTask(taskId).size();
    }
}
