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
    private String condition;
    private Integer maxBorrowDays;
    private Double lateFeePerDay;
    private Double securityDeposit;
    private Integer borrowCount;
    private boolean hasImage;
    private String imageName;
    private String imageType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
