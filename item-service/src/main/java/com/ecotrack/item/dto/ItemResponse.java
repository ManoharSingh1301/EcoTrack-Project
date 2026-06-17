package com.ecotrack.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse implements Serializable {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String category;
    private Boolean available;
    private boolean hasImage;
    private String imageName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
