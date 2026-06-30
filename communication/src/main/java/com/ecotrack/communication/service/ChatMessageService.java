package com.ecotrack.communication.service;

import com.ecotrack.communication.model.ChatMessage;
import com.ecotrack.communication.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final String CHAT_CHANNEL = "chat:messages";

    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        log.info("Saving chat message from user {} to user {}",
                message.getSenderId(), message.getRecipientId());
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        ChatMessage saved = chatMessageRepository.save(message);
        log.debug("Chat message saved with id: {}", saved.getId());

        // Publish to Redis so all service instances can deliver the message
        // to connected WebSocket clients via RedisMessageSubscriber.
        redisTemplate.convertAndSend(CHAT_CHANNEL, saved);
        log.debug("Message {} published to Redis channel '{}'", saved.getId(), CHAT_CHANNEL);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(Long user1, Long user2, Long itemId) {
        log.info("Fetching chat history between users {} and {}, itemId: {}", user1, user2, itemId);
        if (itemId != null) {
            return chatMessageRepository.findChatHistoryForItem(user1, user2, itemId);
        }
        return chatMessageRepository.findChatHistory(user1, user2);
    }
}
