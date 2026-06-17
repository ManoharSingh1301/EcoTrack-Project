package com.ecotrack.item.dto;

import com.ecotrack.item.model.Item;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    public Item toEntity(ItemRequest request) {
        Item item = new Item();
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setOwnerId(request.getOwnerId());
        item.setCategory(request.getCategory());
        item.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        return item;
    }

    public ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .ownerId(item.getOwnerId())
                .category(item.getCategory())
                .available(item.getAvailable())
                .hasImage(item.hasImage())
                .imageName(item.getImageName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public void updateEntity(Item item, ItemRequest request) {
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory());
        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }
    }
}
