package com.microel.trackerbackend.storage.dto.chat;

import com.microel.trackerbackend.storage.dto.team.EmployeeDto;
import lombok.*;

import java.sql.Timestamp;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ChatDto {
    private Long chatId;
    private String title;
    private Timestamp created;
    private EmployeeDto creator;
    private Boolean deleted;
    private ChatMessageDto lastMessage;
    private Set<EmployeeDto> members;
    private Timestamp updated;
    private Timestamp closed;
}
