package com.ecotrack.communication.service;

import com.ecotrack.communication.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Listens on the "chat:messages" Redis channel and delivers each message
 * to the sender and recipient over their WebSocket queues.
 *
 * Because this listener runs on every service instance that subscribes to
 * the channel, messages sent from one instance are delivered to clients
 * connected to any instance — enabling horizontal scaling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Called by the RedisMessageListenerContainer (via MessageListenerAdapter)
     * whenever a new message arrives on "chat:messages".
     *
     * @param message the raw JSON string published to the Redis channel
     */
    public void onMessage(String message) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(chatMessage.getRecipientId()),
                    "/queue/messages",
                    chatMessage
            );

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(chatMessage.getSenderId()),
                    "/queue/messages",
                    chatMessage
            );

            log.debug("Redis subscriber: delivered message {} to users {} and {}",
                    chatMessage.getId(), chatMessage.getSenderId(), chatMessage.getRecipientId());

        } catch (Exception e) {
            log.error("Failed to process Redis message from channel 'chat:messages': {}", e.getMessage(), e);
        }
    }
}
