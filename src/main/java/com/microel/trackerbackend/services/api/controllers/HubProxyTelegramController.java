package com.microel.trackerbackend.services.api.controllers;

import com.microel.tdo.network.NetworkMediaGroup;
import com.microel.tdo.network.NetworkSendPhoto;
import com.microel.trackerbackend.controllers.telegram.UserTelegramController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.List;

@RestController
@RequestMapping("/api/public/telegram")
public class HubProxyTelegramController {
    private final UserTelegramController userTelegramController;

    public HubProxyTelegramController(UserTelegramController userTelegramController) {
        this.userTelegramController = userTelegramController;
    }

    @PostMapping("send-message")
    public ResponseEntity<Message> sendMessage(@RequestBody SendMessage message) throws TelegramApiException {
        return ResponseEntity.ok(userTelegramController.sendMessage(message));
    }

    @PostMapping("send-media-group")
    public ResponseEntity<List<Message>> sendMediaGroup(@RequestBody NetworkMediaGroup message) throws TelegramApiException {
        return ResponseEntity.ok(userTelegramController.sendMediaGroup(message));
    }

    @PostMapping("send-photo")
    public ResponseEntity<Message> sendPhoto(@RequestBody NetworkSendPhoto message) throws TelegramApiException {
        return ResponseEntity.ok(userTelegramController.sendPhoto(message));
    }

    @PostMapping("edit-message-text")
    public ResponseEntity<Serializable> editMessageText(@RequestBody EditMessageText message) throws TelegramApiException {
        return ResponseEntity.ok(userTelegramController.editMessageText(message));
    }

    @PostMapping("edit-message-caption")
    public ResponseEntity<Serializable> editMessageCaption(@RequestBody EditMessageCaption message) throws TelegramApiException {
        return ResponseEntity.ok(userTelegramController.editMessageCaption(message));
    }

    @PostMapping("delete-message")
    public ResponseEntity<Serializable> deleteMessage(@RequestBody DeleteMessage message) throws TelegramApiException {
        return ResponseEntity.ok(userTelegramController.deleteMessage(message));
    }

    @PostMapping("get-file")
    public ResponseEntity<String> getFile(@RequestBody GetFile getFile) throws TelegramApiException {
        return ResponseEntity.ok(userTelegramController.getFile(getFile));
    }
}
