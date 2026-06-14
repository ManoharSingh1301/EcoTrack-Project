package com.service;

import com.model.ChatMessage;
import com.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getChatHistory(Long user1, Long user2, Long itemId) {
        if (itemId != null) {
            return chatMessageRepository.findChatHistoryForItem(user1, user2, itemId);
        }
        return chatMessageRepository.findChatHistory(user1, user2);
    }
}
