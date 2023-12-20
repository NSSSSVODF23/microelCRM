package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.misc.CustomContentPage;
import com.microel.trackerbackend.storage.OffsetPageable;
import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.dto.chat.TelegramMessageBindDto;
import com.microel.trackerbackend.storage.dto.mapper.ChatMessageMapper;
import com.microel.trackerbackend.storage.entities.chat.*;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import com.microel.trackerbackend.storage.exceptions.NotOwner;
import com.microel.trackerbackend.storage.repositories.ChatMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microel.trackerbackend.controllers.telegram.TlgMessageType.TEXT;

@Component
@Transactional(readOnly = true)
public class ChatMessageDispatcher {
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageDispatcher(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public SuperMessage getChatMessage(Long chatMessageId){
        ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId).orElseThrow(()->new EntryNotFound("Сообщение не найдено"));
        if(chatMessage.isGroupMessage()){
            List<ChatMessage> messages = chatMessageRepository.findAllByMediaGroup(chatMessage.getMediaGroup());
            return new ChatMessageMediaGroup(messages).getSuperMessage();
        }else {
            return chatMessage.getSuperMessage();
        }
    }

    public List<Attachment> getAttachments(Long chatMessageId){
        ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId).orElseThrow(()->new EntryNotFound("Сообщение не найдено"));
        if(chatMessage.isGroupMessage()){
            List<ChatMessage> messages = chatMessageRepository.findAllByMediaGroup(chatMessage.getMediaGroup());
            return messages.stream().map(ChatMessage::getAttachment).filter(Objects::nonNull).collect(Collectors.toList());
        }else {
            return Stream.of(chatMessage.getAttachment()).collect(Collectors.toList());
        }
    }

    public Page<SuperMessage> getChatMessages(Long chatId, Long first, Integer limit) {
        Page<ChatMessage> chatMessages = chatMessageRepository.findByParentChat_ChatIdAndDeletedIsNull(chatId,
                new OffsetPageable(first, limit, Sort.by(Sort.Direction.DESC, "sendAt", "chatMessageId")));

        List<ChatMessage> content = new ArrayList<>(chatMessages.getContent());
        if (content.isEmpty()) {
            return Page.empty();
        }
        int lastIndex = content.size() - 1;
        ChatMessage last = content.get(lastIndex);
        if (last.getMediaGroup() != null) {
            List<ChatMessage> byMediaGroup = getByMediaGroup(last.getMediaGroup());
            List<ChatMessage> counter = content.stream().filter(msg -> Objects.equals(msg.getMediaGroup(), last.getMediaGroup())).toList();
            if (counter.size() != byMediaGroup.size()) {
                for (ChatMessage lastMessage : byMediaGroup) {
                    if (!content.contains(lastMessage)) {
                        content.add(lastMessage);
                    }
                }
            }
        }

        List<SuperMessageGetter> groupedMessages = content.stream()
                .filter(ChatMessage::isGroupMessage)
                .collect(Collectors.groupingBy(ChatMessage::getMediaGroup))
                .values()
                .stream()
                .map(ChatMessageMediaGroup::new)
                .collect(Collectors.toList());

        List<SuperMessageGetter> ungroupedMessages = content.stream()
                .filter(ChatMessage::isSoloMessage)
                .collect(Collectors.toList());

        List<SuperMessage> combinedMessages = Stream.concat(groupedMessages.stream(), ungroupedMessages.stream())
                .map(SuperMessageGetter::getSuperMessage)
                .sorted(Comparator.comparing(SuperMessage::getSendAt).thenComparing(SuperMessage::getSuperMessageId).reversed())
                .collect(Collectors.toList());

        return new CustomContentPage<>(combinedMessages, chatMessages, content.size());
    }

    /**
     * Возвращает список сообщений из базы данных отфильтрованных по идентификатору медиа группы
     *
     * @param mediaGroupId Идентификатор медиа группы
     * @return {@link List<ChatMessage>} с найденными в базе данных сообщениями
     */
    public List<ChatMessage> getByMediaGroup(UUID mediaGroupId) {
        return chatMessageRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("mediaGroup"), mediaGroupId));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public List<ChatMessage> getByTelegramMediaGroup(Long chatId, String groupId) {
        return chatMessageRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Long, TelegramMessageBind> telegramMessageBinds = root.join("telegramBinds", JoinType.LEFT);
            predicates.add(cb.equal(telegramMessageBinds.get("telegramChatId"), chatId));
            predicates.add(cb.equal(telegramMessageBinds.get("telegramMediaGroupId"), groupId));
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    @Nullable
    public ChatMessage getByTelegramIds(Message message) {
        if (message.getChatId() == null || message.getMessageId() == null) return null;
        List<ChatMessage> messages = chatMessageRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("telegramChatId"), message.getChatId()));
            predicates.add(cb.equal(root.get("telegramMessageId"), message.getMessageId()));
            if (message.getMediaGroupId() != null && !message.getMediaGroupId().isBlank())
                predicates.add(cb.equal(root.get("telegramMediaGroupId"), message.getMediaGroupId()));

            return cb.and(predicates.toArray(Predicate[]::new));
        });
        if (!messages.isEmpty())
            return messages.get(0);
        else
            return null;
    }

    @Transactional
    public ChatMessage unsafeSave(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    @Transactional
    public List<ChatMessage> unsafeSaveAll(List<ChatMessage> messages) {
        return chatMessageRepository.saveAll(messages);
    }

    public ChatMessage get(Long messageId) throws EntryNotFound {
        return chatMessageRepository.findById(messageId).orElseThrow(() -> new EntryNotFound("Сообщение не найдено"));
    }

    public boolean mediaGroupAlreadyExist(Long chatId, String mediaGroupId) {
        return chatMessageRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Long, TelegramMessageBind> telegramMessageBinds = root.join("telegramBinds", JoinType.LEFT);
            predicates.add(cb.equal(telegramMessageBinds.get("telegramChatId"), chatId));
            predicates.add(cb.equal(telegramMessageBinds.get("telegramMediaGroupId"), mediaGroupId));
            return cb.and(predicates.toArray(Predicate[]::new));
        }).size() > 0;
    }

    public List<ChatMessage> getByBindings(Set<TelegramMessageBindDto> bindings) {
        return chatMessageRepository.findAll((root, query, cb) -> {
            Join<Long, TelegramMessageBind> telegramMessageBinds = root.join("telegramBinds", JoinType.LEFT);
            List<Predicate> predicates = bindings.stream()
                    .map(b -> cb.equal(telegramMessageBinds.get("telegramMessageBindId"), b.getTelegramMessageBindId()))
                    .collect(Collectors.toList());
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    public List<ChatMessage> getByIds(List<Long> messageIds) {
        return chatMessageRepository.findAllById(messageIds);
    }

    public List<ChatMessage> getByIds(Set<Long> messageIds) {
        return chatMessageRepository.findAllById(messageIds);
    }

    /**
     * Назначает установленный идентификатор группе сообщений для их объединения в одно медиа сообщение
     *
     * @param chatMessageIds Список идентификаторов сообщений для объединения
     * @param mediaGroupId   Сгенерированный идентификатор группы
     */
    @Transactional
    public void assignMediaGroup(List<Long> chatMessageIds, UUID mediaGroupId) {
        List<ChatMessage> foundMessages = chatMessageRepository.findAllById(chatMessageIds);
        foundMessages.forEach(m -> m.setMediaGroup(mediaGroupId));
        chatMessageRepository.saveAll(foundMessages);
    }

    /**
     * Устанавливает метку "прочитано" на сообщения
     *
     * @param messageIds Идентификаторы сообщений
     * @param employee   Кто прочитал сообщения
     * @return Список сообщений которые были изменены
     */
    @Transactional
    public List<ChatMessage> setMessagesAsRead(Set<Long> messageIds, Employee employee) {
        List<ChatMessage> foundMessages = chatMessageRepository.findAllById(messageIds);
        List<ChatMessage> changed = foundMessages.stream()
                .filter(m -> m.getReadByEmployees().add(employee))
                .collect(Collectors.toList());
        return chatMessageRepository.saveAll(changed);
    }

    /**
     * Устанавливает метку "прочитано" на <b>все</b> сообщения из чата
     *
     * @param chatId   Идентификатор чата
     * @param employee Кто прочитал сообщения
     * @return Список прочитанных сообщений
     */
    @Transactional
    public List<ChatMessage> setAllMessagesAsRead(Long chatId, Employee employee) {
        List<ChatMessage> foundMessages = chatMessageRepository.findAllByParentChat_ChatIdAndDeletedIsNull(chatId);
        List<ChatMessage> changed = foundMessages.stream()
                .filter(m -> m.getReadByEmployees().add(employee))
                .collect(Collectors.toList());
        return chatMessageRepository.saveAll(changed);
    }

    /**
     * Получает количество не прочитанных сообщений из чата
     *
     * @param chatId   Идентификатор чата
     * @param employee Сотрудник для которого нужно получить кол-во
     * @return Количество не прочитанных сообщений в чате
     */
    public Long getUnreadMessagesCount(Long chatId, Employee employee) {
        return chatMessageRepository.countByParentChat_ChatIdAndReadByEmployeesNotContainsAndDeletedIsNull(chatId, employee);
    }

    /**
     * Возвращает список сообщений связанных с целевым группой
     *
     * @param messageId Идентификатор сообщения
     * @return Список сообщений
     * @throws EntryNotFound Если не найдено целевое сообщение в базе данных
     */
    public List<ChatMessage> getListOfRelatedMessages(Long messageId) throws EntryNotFound {
        ChatMessage chatMessage = get(messageId);
        List<ChatMessage> list = new ArrayList<>();
        if (chatMessage.isGroupMessage()) {
            list = getByMediaGroup(chatMessage.getMediaGroup());
        }
        list.add(chatMessage);
        return list;
    }

    /**
     * Ищет сообщение в базе данных на основе идентификаторов привязки к сообщению из Telegram Api
     *
     * @param chatId       Идентификатор чата из Telegram Api
     * @param messageId    Идентификатор сообщения из Telegram Api
     * @param mediaGroupId Идентификатор медиа-группы из Telegram Api
     * @return {@link ChatMessage} из базы данных
     */
    public ChatMessage getMessageByBind(Long chatId, Integer messageId, String mediaGroupId) throws EntryNotFound {
        return chatMessageRepository.findFirstByTelegramBinds_TelegramChatIdAndTelegramBinds_TelegramMessageIdAndTelegramBinds_TelegramMediaGroupId(chatId, messageId, mediaGroupId)
                .orElseThrow(() -> new EntryNotFound("Сообщение не найдено"));
    }

    @Transactional
    public SuperMessage updateMessageFromTlg(Message receivedMessage, TelegramController context) throws EntryNotFound, TelegramApiException, IllegalFields {
        ChatMessage messageByBind = getMessageByBind(receivedMessage.getChatId(), receivedMessage.getMessageId(), receivedMessage.getMediaGroupId());
        if (Utils.getTlgMsgType(receivedMessage) == TEXT) {
            messageByBind.setText(receivedMessage.getText());
            context.editTextBroadcastMessage(messageByBind, receivedMessage.getChatId());
        } else {
            // Получаем вложения из телеграм сообщения
            Attachment attachmentFromMessage = context.getAttachmentFromMessage(receivedMessage);
            if (!messageByBind.getText().equals(receivedMessage.getCaption())) {
                messageByBind.setText(receivedMessage.getCaption());
                context.editCaptionBroadcastMessage(messageByBind, receivedMessage.getChatId());
            }
            if(!messageByBind.getAttachment().equals(attachmentFromMessage)){
                messageByBind.setAttachment(attachmentFromMessage);
                context.editMediaBroadcastMessage(messageByBind, receivedMessage.getChatId());
            }
        }
        messageByBind.setEdited(Timestamp.from(Instant.now()));
        unsafeSave(messageByBind);
        if (messageByBind.isGroupMessage()) {
            List<ChatMessage> mediaGroup = getByMediaGroup(messageByBind.getMediaGroup());
            return new ChatMessageMediaGroup(mediaGroup).getSuperMessage();
        } else {
            return messageByBind.getSuperMessage();
        }
    }

    @Transactional
    public SuperMessage updateMessageFromWeb(Long editMessageId, String text, Employee author, TelegramController context) throws EntryNotFound, NotOwner, IllegalFields, TelegramApiException {
        ChatMessage target = get(editMessageId);
        if (!target.getAuthor().equals(author))
            throw new NotOwner("Вы не являетесь автором этого сообщения");
        if (Objects.isNull(text) || text.isBlank())
            throw new IllegalFields("Пустое сообщение");
        if (target.getText().equals(text))
            throw new IllegalFields("Текст сообщения не изменен");

        target.setText(text);
        target.setEdited(Timestamp.from(Instant.now()));
        unsafeSave(target);

        context.editTextBroadcastMessage(target, 0L);

        if (target.isGroupMessage()) {
            List<ChatMessage> mediaGroup = getByMediaGroup(target.getMediaGroup());
            return new ChatMessageMediaGroup(mediaGroup).getSuperMessage();
        } else {
            return target.getSuperMessage();
        }
    }
}
