package com.ecotrack.communication.service;

import com.ecotrack.communication.model.ChatMessage;
import com.ecotrack.communication.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        log.info("Saving chat message from user {} to user {}",
                message.getSenderId(), message.getRecipientId());
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        ChatMessage saved = chatMessageRepository.save(message);
        log.debug("Chat message saved with id: {}", saved.getId());

        // Deliver over the in-memory STOMP broker to both participants.
        // (Previously relayed via Redis pub/sub for multi-instance scale-out;
        //  now delivered directly, which is correct for a single instance.)
        messagingTemplate.convertAndSendToUser(
                String.valueOf(saved.getRecipientId()), "/queue/messages", saved);
        if (!saved.getRecipientId().equals(saved.getSenderId())) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(saved.getSenderId()), "/queue/messages", saved);
        }

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
