package com.ecotrack.communication.controller;

import com.ecotrack.communication.model.ChatMessage;
import com.ecotrack.communication.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
// CORS is handled globally via CorsConfig — no @CrossOrigin annotation needed here.
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, java.security.Principal principal) {
        if (principal != null && principal.getName() != null) {
            try {
                Long authenticatedUserId = Long.parseLong(principal.getName());
                if (!authenticatedUserId.equals(chatMessage.getSenderId())) {
                    log.warn("User {} attempted to spoof message as user {}. Overriding senderId.",
                             authenticatedUserId, chatMessage.getSenderId());
                    chatMessage.setSenderId(authenticatedUserId);
                }
            } catch (NumberFormatException e) {
                // This should not happen because CustomHandshakeHandler now validates
                // the userId, but we guard here as a safety net.
                log.warn("Principal name '{}' is not a valid Long — skipping sender ID verification.",
                         principal.getName());
            }
        }

        log.info("WebSocket message received from user {} to user {}",
                chatMessage.getSenderId(), chatMessage.getRecipientId());

        // Save the message and publish to Redis.
        // Delivery to the WebSocket clients is handled by RedisMessageSubscriber.
        chatMessageService.saveMessage(chatMessage);

        log.debug("Message saved and dispatched to Redis relay");
    }

    /**
     * Handles any exception thrown during @MessageMapping method processing
     * and logs it instead of silently dropping the error.
     */
    @MessageExceptionHandler
    public void handleWebSocketException(Exception ex) {
        log.error("Error processing WebSocket message: {}", ex.getMessage(), ex);
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
