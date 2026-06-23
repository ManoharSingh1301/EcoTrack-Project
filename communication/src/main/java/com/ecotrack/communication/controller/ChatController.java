package com.ecotrack.communication.controller;

import com.ecotrack.communication.model.ChatMessage;
import com.ecotrack.communication.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, java.security.Principal principal) {
        if (principal != null && principal.getName() != null) {
            Long authenticatedUserId = Long.parseLong(principal.getName());
            if (!authenticatedUserId.equals(chatMessage.getSenderId())) {
                log.warn("User {} attempted to spoof message as user {}. Overriding senderId.", 
                         authenticatedUserId, chatMessage.getSenderId());
                chatMessage.setSenderId(authenticatedUserId);
            }
        }

        log.info("WebSocket message received from user {} to user {}",
                chatMessage.getSenderId(), chatMessage.getRecipientId());

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

        log.debug("Message delivered to both participants");
    }

    @GetMapping("/history/{user1Id}/{user2Id}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable Long user1Id,
            @PathVariable Long user2Id,
            @RequestParam(required = false) Long itemId) {
        List<ChatMessage> history = chatMessageService.getChatHistory(user1Id, user2Id, itemId);
        return ResponseEntity.ok(history);
    }
}
