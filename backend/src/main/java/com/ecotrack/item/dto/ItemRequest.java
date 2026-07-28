package com.ecotrack.item.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    @NotBlank(message = "Category is required")
    private String category;

    private Boolean available = true;

    /** NEW, LIKE_NEW, GOOD, FAIR, WORN. */
    private String condition;

    @Min(value = 1, message = "Borrow duration must be at least 1 day")
    @Max(value = 365, message = "Borrow duration cannot exceed 365 days")
    private Integer maxBorrowDays;

    @PositiveOrZero(message = "Late fee cannot be negative")
    private Double lateFeePerDay;

    @PositiveOrZero(message = "Security deposit cannot be negative")
    private Double securityDeposit;
}
