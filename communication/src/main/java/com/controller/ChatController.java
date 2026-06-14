package com.controller;

import com.model.ChatMessage;
import com.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        ChatMessage saved = chatMessageService.saveMessage(chatMessage);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getRecipientId()),
                "/queue/messages",
                saved
        );

        messagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getSenderId()),
                "/queue/messages",
                saved
        );
    }

    @GetMapping("/api/chat/history/{user1Id}/{user2Id}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable Long user1Id,
            @PathVariable Long user2Id,
            @RequestParam(required = false) Long itemId) {

        List<ChatMessage> history = chatMessageService.getChatHistory(user1Id, user2Id, itemId);
        return ResponseEntity.ok(history);
    }
}
