package com.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_sender_recipient", columnList = "senderId, recipientId"),
        @Index(name = "idx_chat_item_id", columnList = "itemId"),
        @Index(name = "idx_chat_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Sender ID is required")
    @Column(nullable = false)
    private Long senderId;

    @NotNull(message = "Recipient ID is required")
    @Column(nullable = false)
    private Long recipientId;

    private Long itemId;

    @NotBlank(message = "Message content is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
