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
        if (request.getCondition() != null) item.setCondition(request.getCondition());
        if (request.getMaxBorrowDays() != null) item.setMaxBorrowDays(request.getMaxBorrowDays());
        if (request.getLateFeePerDay() != null) item.setLateFeePerDay(request.getLateFeePerDay());
        if (request.getSecurityDeposit() != null) item.setSecurityDeposit(request.getSecurityDeposit());
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
                .condition(item.getCondition())
                .maxBorrowDays(item.getMaxBorrowDays())
                .lateFeePerDay(item.getLateFeePerDay())
                .securityDeposit(item.getSecurityDeposit())
                .borrowCount(item.getBorrowCount())
                .hasImage(item.hasImage())
                .imageName(item.getImageName())
                .imageType(item.getImageType())
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
        if (request.getCondition() != null) item.setCondition(request.getCondition());
        if (request.getMaxBorrowDays() != null) item.setMaxBorrowDays(request.getMaxBorrowDays());
        if (request.getLateFeePerDay() != null) item.setLateFeePerDay(request.getLateFeePerDay());
        if (request.getSecurityDeposit() != null) item.setSecurityDeposit(request.getSecurityDeposit());
        // ownerId is intentionally NOT updated here — ownership transfer is not supported.
        // The ownerId field in ItemRequest is used only during creation (toEntity).
    }
}
