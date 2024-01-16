package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.services.filemanager.FileData;
import com.microel.trackerbackend.services.filemanager.FileSaver;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.FileType;
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

    public AttachmentDispatcher(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    public Attachment getAttachments(String id) throws EntryNotFound {
        Attachment attachment = this.attachmentRepository.findById(id).orElse(null);
        if (attachment == null) throw new EntryNotFound();
        return attachment;
    }

    /**
     * Получает список с данными файлов из web, сортирует и сохраняет их локально.
     * Так же добавляет запись в таблицу вложений о сохраненном файле
     *
     * @param files Список {@link FileData} с данными фалов из web
     * @return Список сохраненных в базе данных {@link Attachment}
     * @throws EmptyFile  Попытка сохранить пустой файл
     * @throws WriteError Если произошла ошибка при сохранении файла на диск
     */
    public List<Attachment> saveAttachments(List<FileData> files) throws EmptyFile, WriteError {
        List<Attachment> result = new ArrayList<>();
        for (FileData file : files) {

            Timestamp modifiedTimestamp = Timestamp.from(Instant.ofEpochMilli(file.getModified()));
            FileType fileType = FileSaver.getFileType(file);

            Attachment.AttachmentBuilder attachBuilder = Attachment.builder()
                    .type(fileType)
                    .mimeType(file.getType())
                    .createdAt(Timestamp.from(Instant.now()))
                    .modifiedAt(modifiedTimestamp)
                    .size((long) file.getData().length);

            Attachment foundAttachment = attachmentRepository.findById(file.getName()).orElse(null);
            FileSaver fileSaver = new FileSaver();
            if (foundAttachment != null) {
                if (foundAttachment.getModifiedAt().equals(modifiedTimestamp) && foundAttachment.getSize() == file.getData().length) {
                    result.add(foundAttachment);
                } else {
                    String newFileName = UUID.randomUUID() + "_" + file.getName();
                    file.setName(newFileName);
                    FileSaver.ComplexPath path = fileSaver.save(file);
                    result.add(attachmentRepository.save(attachBuilder.name(newFileName).path(path.getOriginal()).thumbnail(path.getThumbnail()).build()));
                }
            } else {
                FileSaver.ComplexPath path = fileSaver.save(file);
                result.add(attachmentRepository.save(attachBuilder.name(file.getName()).path(path.getOriginal()).thumbnail(path.getThumbnail()).build()));
            }

        }

        return result;
    }

    public List<Attachment> getByTask(Long taskId) {
        return attachmentRepository.findAll(
                (root, query, cb) ->
                        cb.and(cb.equal(root.join("comments").join("parent").get("taskId"), taskId)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Integer getCountByTask(Long taskId) {
        return getByTask(taskId).size();
    }

    public Attachment saveAttachment(FileData fileData) throws EmptyFile, WriteError {
        Timestamp modifiedTimestamp = Timestamp.from(Instant.ofEpochMilli(fileData.getModified()));

        Attachment.AttachmentBuilder attachBuilder = Attachment.builder()
                .type(FileSaver.getFileType(fileData))
                .mimeType(fileData.getType())
                .createdAt(Timestamp.from(Instant.now()))
                .modifiedAt(modifiedTimestamp)
                .size((long) fileData.getData().length);

        Attachment foundAttachment = attachmentRepository.findById(fileData.getName()).orElse(null);
        FileSaver fileSaver = new FileSaver();

        if (foundAttachment != null) {
            if (foundAttachment.getModifiedAt().equals(modifiedTimestamp) && foundAttachment.getSize() == fileData.getData().length) {
                return foundAttachment;
            } else {
                String newFileName = UUID.randomUUID() + "_" + fileData.getName();
                fileData.setName(newFileName);
                FileSaver.ComplexPath path = fileSaver.save(fileData);
                return attachmentRepository.save(attachBuilder.name(newFileName).path(path.getOriginal()).thumbnail(path.getThumbnail()).build());
            }
        } else {
            FileSaver.ComplexPath path = fileSaver.save(fileData);
            return attachmentRepository.save(attachBuilder.name(fileData.getName()).path(path.getOriginal()).thumbnail(path.getThumbnail()).build());
        }
    }
}
