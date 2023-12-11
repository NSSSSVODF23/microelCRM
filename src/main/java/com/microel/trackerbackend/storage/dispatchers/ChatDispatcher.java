package com.microel.trackerbackend.storage.dispatchers;

import com.microel.trackerbackend.controllers.telegram.TelegramController;
import com.microel.trackerbackend.controllers.telegram.TlgMessageType;
import com.microel.trackerbackend.controllers.telegram.Utils;
import com.microel.trackerbackend.storage.dto.chat.ChatDto;
import com.microel.trackerbackend.storage.dto.chat.ChatMessageDto;
import com.microel.trackerbackend.storage.dto.chat.TelegramMessageBindDto;
import com.microel.trackerbackend.storage.dto.comment.AttachmentDto;
import com.microel.trackerbackend.storage.dto.mapper.AttachmentMapper;
import com.microel.trackerbackend.storage.dto.mapper.ChatMapper;
import com.microel.trackerbackend.storage.dto.mapper.ChatMessageMapper;
import com.microel.trackerbackend.storage.entities.chat.*;
import com.microel.trackerbackend.storage.entities.comments.Attachment;
import com.microel.trackerbackend.storage.entities.team.Employee;
import com.microel.trackerbackend.storage.exceptions.*;
import com.microel.trackerbackend.storage.repositories.ChatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Transactional(readOnly = true)
public class ChatDispatcher {
    private final ChatRepository chatRepository;
    private final ChatMessageDispatcher chatMessageDispatcher;

    public ChatDispatcher(ChatRepository chatRepository, ChatMessageDispatcher chatMessageDispatcher) {
        this.chatRepository = chatRepository;
        this.chatMessageDispatcher = chatMessageDispatcher;
    }

    public Chat getChat(Long chatId) throws EntryNotFound {
        return chatRepository.findById(chatId).orElseThrow(() -> new EntryNotFound("Чат не найден"));
    }

    public ChatDto getChatDto(Long chatId) throws EntryNotFound {
        return ChatMapper.toDto(getChat(chatId));
    }

    /**
     * Сохраняет чат в базе данных, метод ничего не проверяет
     *
     * @return Сохраненный чат
     */
    public Chat unsafeSave(Chat chat) {
        return chatRepository.save(chat);
    }

    public ChatMessage getMessageByTelegram(Message message) {
        return chatMessageDispatcher.getByTelegramIds(message);
    }

    /**
     * Возвращает страницу с {@link SuperMessage} по идентификатору чата, для отправки в web
     *
     * @param chatId Идентификатор чата
     * @param first  Смещение
     * @param limit  Кол-во сообщений на странице
     * @return Странница с {@link SuperMessage}
     * @throws EntryNotFound Если чат не найден
     */
    public Page<SuperMessage> getChatMessages(Long chatId, Long first, Integer limit) throws EntryNotFound {
        chatRepository.findById(chatId).orElseThrow(() -> new EntryNotFound("Чат не найден"));
        return chatMessageDispatcher.getChatMessages(chatId, first, limit);
    }

//    /**
//     * Создает текстовое сообщение в базе данных. И транслирует в чаты.
//     *
//     * @param chatId           Идентификатор чата в бд
//     * @param text             Текст сообщения
//     * @param author           Автор сообщения
//     * @param context          Контекст телеграм контроллера для транслирования в tg api
//     * @param replyToMessageId Идентификатор сообщения для ответа (Необязательно)
//     * @return Возвращает созданное сообщение
//     * @throws EntryNotFound Если чат не найден
//     */
//    public SuperMessage createMessage(Long chatId, String text, Employee author,
//                                      @Nullable Long replyToMessageId, TelegramController context) throws TelegramApiException, EntryNotFound {
//        Chat chat = getChat(chatId);
//        ChatMessage replyToMessage = null;
//        if (Objects.nonNull(replyToMessageId)) {
//            try {
//                replyToMessage = chatMessageDispatcher.get(replyToMessageId);
//            } catch (EntryNotFound ignored) {
//                //Если не находим сообщение для ответа, игнорируем
//            }
//        }
//        ChatMessage message = ChatMessage.builder()
//                .text(text)
//                .author(author)
//                .replyTo(replyToMessage)
//                .parentChat(chat)
//                .sendAt(Timestamp.from(Instant.now()))
//                .readByEmployees(Stream.of(author).collect(Collectors.toSet()))
//                .build();
//        chat.setLastMessage(message);
//        chat.setUpdated(Timestamp.from(Instant.now()));
//        unsafeSave(chat);
//        ChatMessage savedMessage = chatMessageDispatcher.unsafeSave(message);
//        // Транслируем во все чаты
//        List<Message> responseMessages = context.sendTextBroadcastMessage(chat, ChatMessageMapper.toDto(savedMessage));
//        // Добавляем связи
//        return appendBindToMessage(savedMessage.getChatMessageId(), responseMessages);
//    }

//    /**
//     * Создает медиа сообщение в базе данных. И транслирует в чаты.
//     *
//     * @param chatId           Идентификатор чата в бд
//     * @param text             Текст сообщения
//     * @param author           Автор сообщения
//     * @param attachment       Вложение
//     * @param context          Контекст телеграм контроллера для транслирования в tg api
//     * @param replyToMessageId Идентификатор сообщения для ответа (Необязательно)
//     * @return Возвращает созданное сообщение
//     * @throws EntryNotFound        Если чат не найден
//     * @throws IllegalFields        Если в сообщении нет вложения
//     * @throws IllegalMediaType     Если задан не верный тип данных для вложения
//     * @throws TelegramApiException Если ошибка отправки сообщения в tg api
//     */
//    public SuperMessage createMessage(Long chatId, String text, Employee author, Attachment attachment,
//                                      @Nullable Long replyToMessageId, TelegramController context) throws TelegramApiException, EntryNotFound, IllegalFields, IllegalMediaType {
//        Chat chat = getChat(chatId);
//        ChatMessage replyToMessage = null;
//        if (Objects.nonNull(replyToMessageId)) {
//            try {
//                replyToMessage = chatMessageDispatcher.get(replyToMessageId);
//            } catch (EntryNotFound ignored) {
//                //Если не находим сообщение для ответа, игнорируем
//            }
//        }
//        ChatMessage message = ChatMessage.builder()
//                .text(text)
//                .author(author)
//                .replyTo(replyToMessage)
//                .attachment(attachment)
//                .parentChat(chat)
//                .sendAt(Timestamp.from(Instant.now()))
//                .readByEmployees(Stream.of(author).collect(Collectors.toSet()))
//                .build();
//        chat.setLastMessage(message);
//        chat.setUpdated(Timestamp.from(Instant.now()));
//        unsafeSave(chat);
//        ChatMessage savedMessage = chatMessageDispatcher.unsafeSave(message);
//        // Транслируем во все чаты
//        List<Message> responseMessages = context.sendMediaBroadcastMessage(chat, ChatMessageMapper.toDto(savedMessage));
//        // Добавляем связи
//        return appendBindToMessage(savedMessage.getChatMessageId(), responseMessages);
//    }

//    /**
//     * Создает групповое медиа сообщение в базе данных. И транслирует в чаты.
//     *
//     * @param chatId           Идентификатор чата в бд
//     * @param text             Текст сообщения
//     * @param author           Автор сообщения
//     * @param attachments      Список вложений
//     * @param context          Контекст телеграм контроллера для транслирования в tg api
//     * @param replyToMessageId Идентификатор сообщения для ответа (Необязательно)
//     * @return Возвращает созданное сообщение
//     * @throws EntryNotFound        Если чат не найден
//     * @throws IllegalFields        Если в сообщении нет вложения
//     * @throws IllegalMediaType     Если задан не верный тип данных для вложения
//     * @throws TelegramApiException Если ошибка отправки сообщения в tg api
//     */
//    public SuperMessage createMessage(Long chatId, String text, Employee author, List<Attachment> attachments,
//                                      @Nullable Long replyToMessageId, TelegramController context) throws TelegramApiException, EntryNotFound, IllegalFields, IllegalMediaType {
//        Chat chat = getChat(chatId);
//        ChatMessage replyToMessage = null;
//        if (Objects.nonNull(replyToMessageId)) {
//            try {
//                replyToMessage = chatMessageDispatcher.get(replyToMessageId);
//            } catch (EntryNotFound ignored) {
//                //Если не находим сообщение для ответа, игнорируем
//            }
//        }
//
//        UUID mediaGroupId = UUID.randomUUID();
//        List<ChatMessage> messages = new ArrayList<>();
//
//        for (Attachment attachment : attachments) {
//            ChatMessage message = ChatMessage.builder()
//                    .text(text)
//                    .author(author)
//                    .replyTo(replyToMessage)
//                    .attachment(attachment)
//                    .parentChat(chat)
//                    .mediaGroup(mediaGroupId)
//                    .sendAt(Timestamp.from(Instant.now()))
//                    .readByEmployees(Stream.of(author).collect(Collectors.toSet()))
//                    .build();
//            messages.add(chatMessageDispatcher.unsafeSave(message));
//        }
//        chat.setLastMessage(messages.get(0));
//        chat.setUpdated(Timestamp.from(Instant.now()));
//        unsafeSave(chat);
//        List<ChatMessage> savedMessages = chatMessageDispatcher.unsafeSaveAll(messages);
//        // Транслируем во все чаты
//        List<List<Message>> responseMessages = context.sendMediaGroupBroadcastMessage(chat, savedMessages.stream().map(ChatMessageMapper::toDto).collect(Collectors.toList()));
//        // Добавляем связи
//        return appendBindToMessage(savedMessages.stream().map(ChatMessage::getChatMessageId).collect(Collectors.toList()), responseMessages);
//    }

    /**
     * Создает текстовое сообщение в базе данных. И транслирует в чаты.
     *
     * @param chatId     Идентификатор чата в бд
     * @param tlgMessage Сообщение полученное от Telegram Api
     * @param author     Автор сообщения
     * @param context    Контекст телеграм контроллера для транслирования в tg api
     * @return Возвращает созданное сообщение
     * @throws EntryNotFound Если чат не найден
     * @throws IllegalFields Если в сообщении нет вложения
     * @throws IllegalMediaType Если задан не верный тип данных для вложения
     * @throws TelegramApiException Если ошибка отправки сообщения в tg api
     */
    @Transactional
    public SuperMessage createMessage(Long chatId, Message tlgMessage, Employee author, TelegramController context) throws TelegramApiException, EntryNotFound, IllegalFields, IllegalMediaType {
        Chat chat = getChat(chatId);
        ChatMessage replyToMessage = null;
        if (Objects.nonNull(tlgMessage.getReplyToMessage())) {
            try {
                Message tlgReplyMessage = tlgMessage.getReplyToMessage();
                replyToMessage = chatMessageDispatcher.getMessageByBind(tlgReplyMessage.getChatId(), tlgReplyMessage.getMessageId(), tlgReplyMessage.getMediaGroupId());
            } catch (EntryNotFound ignored) {
                //Если не находим сообщение для ответа, игнорируем
            }
        }

        Attachment attachmentFromMessage = null;
        if (Utils.getTlgMsgType(tlgMessage) == TlgMessageType.MEDIA) {
            // Получаем вложения из телеграм сообщения
            attachmentFromMessage = context.getAttachmentFromMessage(tlgMessage);
        }

        ChatMessage message = ChatMessage.builder()
                .text(tlgMessage.getText())
                .author(author)
                .replyTo(replyToMessage)
                .attachment(attachmentFromMessage)
                .parentChat(chat)
                .sendAt(Timestamp.from(Instant.now()))
                .readByEmployees(Stream.of(author).collect(Collectors.toSet()))
                .build();
        chat.setLastMessage(message);
        chat.setUpdated(Timestamp.from(Instant.now()));
        unsafeSave(chat);
        ChatMessage savedMessage = chatMessageDispatcher.unsafeSave(message);
        return collectSuperMessage(List.of(savedMessage));
    }

    /**
     * Создает текстовое сообщение в базе данных. И транслирует в чаты.
     *
     * @param chatId     Идентификатор чата в бд
     * @param tlgMessages Список сообщений объединенных медиа группой полученное от Telegram Api
     * @param author     Автор сообщения
     * @param context    Контекст телеграм контроллера для транслирования в tg api
     * @return Возвращает созданное сообщение
     * @throws EntryNotFound Если чат не найден
     * @throws IllegalFields Если в сообщении нет вложения
     * @throws IllegalMediaType Если задан не верный тип данных для вложения
     * @throws TelegramApiException Если ошибка отправки сообщения в tg api
     */
    @Transactional
    public SuperMessage createMessage(Long chatId, List<Message> tlgMessages, Employee author, TelegramController context) throws TelegramApiException, EntryNotFound, IllegalFields, IllegalMediaType {
        Chat chat = getChat(chatId);

        List<ChatMessage> createdMessages = new ArrayList<>();

        UUID mediaGroupId = UUID.randomUUID();

        for (Message message : tlgMessages) {
            ChatMessage replyToMessage = null;
            if (Objects.nonNull(message.getReplyToMessage())) {
                try {
                    Message tlgReplyMessage = message.getReplyToMessage();
                    replyToMessage = chatMessageDispatcher.getMessageByBind(tlgReplyMessage.getChatId(), tlgReplyMessage.getMessageId(), tlgReplyMessage.getMediaGroupId());
                } catch (EntryNotFound ignored) {
                    //Если не находим сообщение для ответа, игнорируем
                }
            }
            Attachment attachmentFromGroupMessage = context.getAttachmentFromMessage(message);
            createdMessages.add(ChatMessage.builder()
                    .text(message.getText())
                    .author(author)
                    .replyTo(replyToMessage)
                    .attachment(attachmentFromGroupMessage)
                    .parentChat(chat)
                    .sendAt(Timestamp.from(Instant.now()))
                    .readByEmployees(Stream.of(author).collect(Collectors.toSet()))
                    .mediaGroup(mediaGroupId)
                    .build());
        }

        chat.setLastMessage(createdMessages.get(0));
        unsafeSave(chat);
        List<ChatMessage> savedMessages = chatMessageDispatcher.unsafeSaveAll(createdMessages);

        return collectSuperMessage(savedMessages);
    }

    public SuperMessage createSystemMessage(Long chatId, String message, TelegramController context) throws EntryNotFound, TelegramApiException {
        Chat chat = getChat(chatId);
        ChatMessage systemMessage = ChatMessage.builder()
                .text(message)
                .author(Employee.getSystem())
                .parentChat(chat)
                .sendAt(Timestamp.from(Instant.now()))
                .build();
        chat.setLastMessage(systemMessage);
        chat.setUpdated(Timestamp.from(Instant.now()));
        unsafeSave(chat);
        ChatMessage chatMessage = chatMessageDispatcher.unsafeSave(systemMessage);
//        List<Message> messageList = context.sendTextBroadcastMessage(chat, ChatMessageMapper.toDto(chatMessage));
        return chatMessage.getSuperMessage();
    }

//    /**
//     * Добавляет привязку к сообщениям из telegram api в базу данных
//     *
//     * @param messageId   Идентификатор сообщения к которому нужно добавить привязку
//     * @param sentMessage Объект ответа от telegram api в котором хранятся необходимые данные
//     * @return Возвращает идентификатор сообщения с добавленной привязкой
//     * @throws EntryNotFound Если сообщение для добавления привязки не найдено в бд
//     */
//    public Long appendBindToMessage(Long messageId, Message sentMessage) throws EntryNotFound {
//        ChatMessage existedMessage = chatMessageDispatcher.get(messageId);
//        existedMessage.appendBind(TelegramMessageBind.from(sentMessage));
//        return chatMessageDispatcher.unsafeSave(existedMessage).getChatMessageId();
//    }

//    /**
//     * Добавляет несколько привязок из разных чатов telegram к одному сообщению из базы данных
//     *
//     * @param messageId    Идентификатор сообщения к которому нужно добавить привязку
//     * @param sentMessages Список объектов ответа от telegram api в которых хранятся необходимые данные
//     * @return Возвращает объект SuperMessage созданный на основе сообщения к которому была произведена привязка
//     * @throws EntryNotFound Если сообщение для добавления привязки не найдено в бд
//     */
//    @Nullable
//    public SuperMessage appendBindToMessage(Long messageId, List<Message> sentMessages) throws EntryNotFound {
//        for (Message message : sentMessages) {
//            appendBindToMessage(messageId, message);
//        }
//        return collectSuperMessageByIds(messageId);
//    }

//    /**
//     * Добавляет несколько привязок из разных чатов telegram к группе сообщений (медиа группа) из базы данных
//     *
//     * @param mediaGroupMessagesIds Список идентификаторов к которым нужно добавить привязку
//     * @param sentMessages          Список объектов ответа от telegram api в которых хранятся необходимые данные (Двухмерный список, Сообщение -> [...Ответ])
//     * @return Возвращает объект SuperMessage созданный на основе сообщений к которым была произведена привязка
//     * @throws EntryNotFound Если сообщение для добавления привязки не найдено в бд
//     */
//    @Nullable
//    public SuperMessage appendBindToMessage(List<Long> mediaGroupMessagesIds, List<List<Message>> sentMessages) throws EntryNotFound {
//        for (int i = 0; i < mediaGroupMessagesIds.size(); i++) {
//            Long messageId = mediaGroupMessagesIds.get(i);
//            List<Message> messageList = sentMessages.get(i);
//            appendBindToMessage(messageId, messageList);
//        }
//        return collectSuperMessageByIds(mediaGroupMessagesIds);
//    }


    public ChatMessageDto updateMessage(Long messageId, ChatMessage message) throws EntryNotFound {
        ChatMessage existedMessage = chatMessageDispatcher.get(messageId);
        message.setText(message.getText());
        message.setAttachment(message.getAttachment());
        message.setEdited(Timestamp.from(Instant.now()));
        return ChatMessageMapper.toDto(chatMessageDispatcher.unsafeSave(existedMessage));
    }

    /**
     * Удаляет сообщение из чата
     *
     * @param messageId Идентификатор целевого сообщения
     * @param employee  Сотрудник инициирующий удаление
     * @return Удаленное сообщение
     * @throws EntryNotFound  Если сообщение не найдено
     * @throws NotOwner       Если сотрудники не являются владельцем сообщения
     * @throws AlreadyDeleted Если сообщение уже удалено
     */

    public ChatMessage deleteMessage(Long messageId, Employee employee) throws EntryNotFound, NotOwner, AlreadyDeleted {
        ChatMessage existedMessage = chatMessageDispatcher.get(messageId);
        if (existedMessage.getDeleted() != null) throw new AlreadyDeleted("Сообщение уже удалено");
        if (!existedMessage.getAuthor().equals(employee)) throw new NotOwner("Вы не являетесь владельцем сообщения");
        existedMessage.setDeleted(Timestamp.from(Instant.now()));
        return chatMessageDispatcher.unsafeSave(existedMessage);
    }


    public List<ChatMessageDto> getMessagesByMediaGroup(Long telegramChatId, String mediaGroupId) throws EntryNotFound {
        List<ChatMessage> messageList = chatMessageDispatcher.getByTelegramMediaGroup(telegramChatId, mediaGroupId);
        if (messageList.isEmpty())
            throw new EntryNotFound("Сообщения в telegram чате " + telegramChatId + " и с медиа группой " + mediaGroupId + " не найдены");
        return messageList.stream().map(ChatMessageMapper::toDto).collect(Collectors.toList());
    }

    @Nullable
    public SuperMessage getMessageByMediaGroup(Long telegramChatId, String mediaGroupId) {
        List<ChatMessage> mediaGroup = chatMessageDispatcher.getByTelegramMediaGroup(telegramChatId, mediaGroupId);
        return collectSuperMessage(mediaGroup);
    }

    @Nullable
    public SuperMessage getMessageByMediaGroup(List<ChatMessageDto> messages) {
        if (messages == null || messages.isEmpty())
            throw new NullPointerException("Нет сообщений для получения группы");
        Set<Set<TelegramMessageBindDto>> bindings = messages.stream().map(ChatMessageDto::getTelegramBinds).collect(Collectors.toSet());
        if (bindings.size() != 1)
            throw new IllegalStateException("Попытка получить разные медиа группы в одном сообщении");
        Set<TelegramMessageBindDto> messageBindDtoSet = bindings.iterator().next();
        List<ChatMessage> mediaGroup = chatMessageDispatcher.getByBindings(messageBindDtoSet);
        return collectSuperMessage(mediaGroup);
    }

    @Nullable
    public SuperMessage collectSuperMessage(List<ChatMessage> messages) {
        SuperMessage superMessage = null;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage chatMessage = messages.get(i);
            if (i == 0) {
                superMessage = chatMessage.getSuperMessage();
            } else {
                superMessage.getAttachments().add(AttachmentMapper.toDto(chatMessage.getAttachment()));
                superMessage.getIncludedMessages().add(chatMessage.getChatMessageId());
            }
        }
        return superMessage;
    }

    @Nullable
    public SuperMessage collectSuperMessageByIds(Long messageId) {
        try {
            ChatMessage chatMessage = chatMessageDispatcher.get(messageId);
            return chatMessage.getSuperMessage();
        } catch (EntryNotFound ignored) {
        }
        return null;
    }

    @Nullable
    public SuperMessage collectSuperMessageByIds(List<Long> messageIds) {
        List<ChatMessage> messages = chatMessageDispatcher.getByIds(messageIds);
        return collectSuperMessage(messages);
    }

    @Nullable
    public SuperMessage collectSuperMessageByIds(Set<Long> messageIds) {
        List<ChatMessage> messages = chatMessageDispatcher.getByIds(messageIds);
        return collectSuperMessage(messages);
    }

    /**
     * Проверяет наличие существующей медиа группы из телеграмм
     *
     * @param chatId       Идентификатор чата из телеграм
     * @param mediaGroupId Идентификатор медиа группы из телеграм
     * @return Возвращает true если медиа группа существует
     */
    public boolean mediaGroupAlreadyExist(Long chatId, String mediaGroupId) {
        return chatMessageDispatcher.mediaGroupAlreadyExist(chatId, mediaGroupId);
    }

    /**
     * Создает уникальны идентификатор группы и вызывает метод в {@link ChatMessageDispatcher} для назначения идентификатора группе медиа сообщений
     *
     * @param chatMessageIds Список идентификаторов сообщений которые нужно объединить в группу
     */

    public void assignMediaGroup(List<Long> chatMessageIds) {
        UUID mediaGroupId = UUID.randomUUID();
        chatMessageDispatcher.assignMediaGroup(chatMessageIds, mediaGroupId);
    }

    /**
     * Проксирует запрос в {@link ChatMessageDispatcher} на установку метки прочитано на сообщения в чате
     *
     * @param messageIds Идентификаторы сообщений
     * @param employee   Кто прочитал сообщения
     * @return Супер сообщение
     */

    @Nullable
    public SuperMessage setMessagesAsRead(Set<Long> messageIds, Employee employee) {
        List<ChatMessage> chatMessages = chatMessageDispatcher.setMessagesAsRead(messageIds, employee);
        if (chatMessages.isEmpty()) return null;
        return new ChatMessageMediaGroup(chatMessages).getSuperMessage();
    }

    /**
     * Проксирует запрос в {@link ChatMessageDispatcher} на установку метки прочитано на <b>все</b> сообщения в чате
     *
     * @param chatId   Идентификатор чата
     * @param employee Кто прочитал сообщения
     * @return Супер сообщения
     */

    public List<SuperMessage> setAllMessagesAsRead(Long chatId, Employee employee) {
        List<ChatMessage> chatMessages = chatMessageDispatcher.setAllMessagesAsRead(chatId, employee);
        List<SuperMessage> groupedMessages = chatMessages.stream()
                .filter(ChatMessage::isGroupMessage)
                .collect(Collectors.groupingBy(ChatMessage::getMediaGroup))
                .values().stream().map(ChatMessageMediaGroup::new)
                .map(ChatMessageMediaGroup::getSuperMessage).collect(Collectors.toList());
        List<SuperMessage> soloMessages = chatMessages.stream()
                .filter(chatMessage -> chatMessage.getMediaGroup() == null)
                .map(ChatMessage::getSuperMessage).toList();

        groupedMessages.addAll(soloMessages);
        groupedMessages.sort(Comparator.comparing(SuperMessage::getSuperMessageId).thenComparing(SuperMessage::getSendAt));
        return groupedMessages;
    }

    /**
     * Проксикует запрос на кол-во не прочитанных сообщений
     *
     * @param chatId   Идентификатор чата
     * @param employee Сотрудник для которого нужно получить кол-во
     * @return Кол-воп не прочитанных сообщений
     */
    public Long getUnreadMessagesCount(Long chatId, Employee employee) {
        return chatMessageDispatcher.getUnreadMessagesCount(chatId, employee);
    }

    /**
     * Принимает идентификатор сообщения и возвращает это сообщение и все сообщения связанные с ним
     *
     * @param messageId Идентификатор сообщения
     * @return Список связанных сообщений
     * @throws EntryNotFound Если целевое сообщение не найдено в базе данных
     */
    public List<ChatMessage> getListOfRelatedMessages(Long messageId) throws EntryNotFound {
        return chatMessageDispatcher.getListOfRelatedMessages(messageId);
    }

    /**
     * Проксирует запрос на поиск сообщения по привязке к сообщению из Telegram Api
     *
     * @param chatId       Идентификатор чата из Telegram Api
     * @param messageId    Идентификатор сообщения из Telegram Api
     * @param mediaGroupId Идентификатор медиа-группы из Telegram Api
     * @return {@link ChatMessage} найденное в базе данных
     * @throws EntryNotFound Если сообщение не найдено
     */
    public ChatMessage getMessageByBind(Long chatId, Integer messageId, String mediaGroupId) throws EntryNotFound {
        return chatMessageDispatcher.getMessageByBind(chatId, messageId, mediaGroupId);
    }

    public List<Chat> getMyActiveChats(Employee employee) {
        return chatRepository.findAllByMembersContainingAndClosedIsNull(employee, Sort.by(Sort.Direction.DESC,"updated", "created"));
    }

    public List<Attachment> getAttachments(Long superMessageId) {
        return chatMessageDispatcher.getAttachments(superMessageId);
    }
}
