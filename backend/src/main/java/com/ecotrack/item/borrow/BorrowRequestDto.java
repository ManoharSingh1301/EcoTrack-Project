package com.ecotrack.item.borrow;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload a borrower submits to request an item. */
public class BorrowRequestDto {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Borrow duration is required")
    @Min(value = 1, message = "Borrow duration must be at least 1 day")
    @Max(value = 365, message = "Borrow duration cannot exceed 365 days")
    private Integer borrowDays;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Integer getBorrowDays() { return borrowDays; }
    public void setBorrowDays(Integer borrowDays) { this.borrowDays = borrowDays; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
