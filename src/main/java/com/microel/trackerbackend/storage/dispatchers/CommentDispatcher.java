package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.misc.sorting.TaskJournalSortingTypes;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.services.external.oldtracker.OldTrackerRequestFactory;
import com.microel.trackerbackend.services.filemanager.exceptions.EmptyFile;
import com.microel.trackerbackend.services.filemanager.exceptions.WriteError;
import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.dto.comment.CommentDto;
import com.microel.trackerbackend.storage.dto.mapper.CommentMapper;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.comments.Comment;
import com.microel.trackerbackend.storage.entities.comments.dto.CommentData;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.task.WorkLog;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.entities.team.notification.Notification;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.exceptions.NotOwner;
import com.microel.trackerbackend.storage.repositories.CommentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.Collections;
import java.util.Collections;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CommentDispatcher {
    private final CommentRepository commentRepository;
    private final AttachmentDispatcher attachmentDispatcher;
    private final TaskDispatcher taskDispatcher;
    private final StompController stompController;
    private final NotificationDispatcher notificationDispatcher;
    private final WorkLogDispatcher workLogDispatcher;

    public CommentDispatcher(CommentRepository commentRepository, AttachmentDispatcher attachmentDispatcher,
                             @Lazy TaskDispatcher taskDispatcher, StompController stompController,
                             @Lazy NotificationDispatcher notificationDispatcher, WorkLogDispatcher workLogDispatcher) {
        this.commentRepository = commentRepository;
        this.attachmentDispatcher = attachmentDispatcher;
        this.taskDispatcher = taskDispatcher;
        this.stompController = stompController;
        this.notificationDispatcher = notificationDispatcher;
        this.workLogDispatcher = workLogDispatcher;
    }

    public Page<CommentDto> getComments(Long taskId, Long offset, Integer limit, @Nullable TaskJournalSortingTypes sortingTypes) {
        Sort sort = Sort.by(Sort.Direction.DESC, "created");
        if(sortingTypes != null)
            switch (sortingTypes){
                case CREATE_DATE_ASC -> sort = Sort.by(Sort.Direction.ASC, "created");
                case CREATE_DATE_DESC -> sort = Sort.by(Sort.Direction.DESC, "created");
            }
        return commentRepository.findAllByParent_TaskIdAndDeletedIsFalse(taskId,
                new OffsetPageable(offset, limit, sort)).map(CommentMapper::toDto);
    }

    public Comment create(CommentData data, Employee currentUser) throws EmptyFile, WriteError, EntryNotFound {
        Task targetTask = taskDispatcher.getTask(data.getTaskId());

        Comment replyComment = null;
        if(data.getReplyComment()!=null) {
            replyComment = commentRepository.findById(data.getReplyComment()).orElse(null);
        }

        List<Attachment> attachments = new ArrayList<>();

        if (data.getFiles().size() > 0) {
            attachments = attachmentDispatcher.saveAttachments(data.getFiles());
        }

        Comment comment = commentRepository.save(
                Comment.builder()
                        .deleted(false)
                        .edited(false)
                        .created(Timestamp.from(Instant.now()))
                        .creator(currentUser)
                        .message(data.getText())
                        .replyComment(replyComment)
                        .attachments(attachments)
                        .parent(targetTask)
                        .build()
        );

        targetTask.getLastComments().add(comment);
        if(targetTask.getLastComments().size() > 5)
            targetTask.getLastComments().remove(0);

        Task savedTask = taskDispatcher.unsafeSave(targetTask);

        stompController.createComment(Objects.requireNonNull(CommentMapper.toDto(comment), "Созданные комментарий равен null"), targetTask.getTaskId().toString());
        stompController.updateTask(savedTask);

        if(currentUser.isHasOldTrackerCredentials() && targetTask.getOldTrackerTaskId() != null){
            OldTrackerRequestFactory requestFactory = new OldTrackerRequestFactory(currentUser.getOldTrackerCredentials().getUsername(), currentUser.getOldTrackerCredentials().getPassword());
            requestFactory.createComment(targetTask.getOldTrackerTaskId(), comment.getMessage()).execute();
            requestFactory.close().execute();
        }

        return comment;
    }

    public List<Long> getTaskIdsByGlobalSearch(String globalSearchValue) {
        Page<Comment> comments = commentRepository.findAll(((root, query, cb) ->
                cb.and(cb.isTrue(
                        cb.function("fts", Boolean.class, root.get("message"), cb.literal(globalSearchValue))))
        ), Pageable.unpaged());
        return comments.stream().map(comment -> comment.getParent().getTaskId()).collect(Collectors.toList());
    }

    public Comment update(Comment editedComment, Employee employeeWhoMadeTheChange) throws NotOwner, IllegalFields, EntryNotFound {

        // Check if the comment has an ID, if not throw an exception
        if (editedComment.getCommentId() == null) {
            throw new IllegalFields("Идентификатор комментария не может быть пустым");
        }

        // We check that the employee who made changes to the comment is the owner of this comment,
        // if this is not the case, we throw an exception
        if (!editedComment.getCreator().equals(employeeWhoMadeTheChange)) {
            throw new NotOwner("Вы не являетесь владельцем этого комментария");
        }

        // Check if the comment message is empty throw an exception
        if (editedComment.getMessage().isBlank()) {
            throw new IllegalFields("Сообщение комментария не может быть пустым");
        }

        Comment comment = commentRepository.findById(editedComment.getCommentId()).orElse(null);

        // If a comment with this ID is not found, throw an exception.
        if (comment == null) {
            throw new EntryNotFound("Комментарий с таким идентификатором не найден");
        }

        // Check if the message matches the content of an existing comment
        if (comment.getMessage().equals(editedComment.getMessage())) {
            throw new IllegalFields("Сообщение комментария не было отредактировано");
        }

        comment.setMessage(editedComment.getMessage());
        comment.setEdited(true);

        Comment save = commentRepository.save(comment);
        stompController.updateComment(CommentMapper.toDto(save), comment.getParent().getTaskId().toString());
        stompController.updateTask(comment.getParent());

        return save;
    }

    public Comment delete(Long commentId, Employee currentUser) throws EntryNotFound, NotOwner {
        // Getting a comment from the database by ID
        Comment comment = commentRepository.findById(commentId).orElse(null);

        // Check if the comment has been received, if not, throw an exception
        if (comment == null) {
            throw new EntryNotFound("Комментарий с таким идентификатором не найден");
        }

        // Check if the user is the owner of the comment
        if (!comment.getCreator().equals(currentUser)) {
            throw new NotOwner("Вы не являетесь владельцем этого комментария");
        }

        comment.setDeleted(true);

        Comment save = commentRepository.save(comment);

        setLastCommentsToTask(comment.getParent());

        return save;
    }

    @Transactional
    public void setLastCommentsToTask(Task task){
        Page<Comment> taskComments = commentRepository.findAll((root, query, cb) -> {
            return cb.and(cb.equal(root.get("parent"), task), cb.isFalse(root.get("deleted")));
        }, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "created")));

        task.getLastComments().clear();
        List<Comment> subList = Stream.of(taskComments.getContent().toArray(Comment[]::new)).collect(Collectors.toList());
        Collections.reverse(subList);
        task.getLastComments().addAll(subList);
        stompController.updateTask(taskDispatcher.unsafeSave(task));
    }

    public void attach(Long chatId, List<Attachment> attachments, String description, Employee employee) {
        WorkLog workLog = workLogDispatcher.getByChatId(chatId);
        Task task = workLog.getTask();
        Set<Employee> allEmployeesObservers = task.getAllEmployeesObservers(employee);
        Comment comment = Comment.builder()
                .message(description == null ? "" : description)
                .parent(task)
                .created(Timestamp.from(Instant.now()))
                .attachments(attachments)
                .creator(employee)
                .deleted(false)
                .edited(false)
                .build();
        comment = commentRepository.save(comment);
//        comment.setAttachments(attachments.stream().map(AttachmentMapper::fromDto).collect(Collectors.toList()));

        stompController.createComment(CommentMapper.toDto(comment), String.valueOf(task.getTaskId()));
        notificationDispatcher.createNotification(allEmployeesObservers, Notification.newComment(comment));
    }
}
