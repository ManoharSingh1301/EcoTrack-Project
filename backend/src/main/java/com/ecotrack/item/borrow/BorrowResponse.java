package com.ecotrack.item.borrow;

import java.time.LocalDateTime;

/** Read model returned to the frontend for a borrow request. */
public class BorrowResponse {

    private Long id;
    private Long itemId;
    private String itemName;
    private Long ownerId;
    private Long borrowerId;
    private BorrowStatus status;
    private Integer borrowDays;
    private String note;
    private LocalDateTime requestDate;
    private LocalDateTime decisionDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private Double lateFee;
    private Double securityDeposit;
    /** True when the item is borrowed and past its due date. */
    private boolean overdue;

    public static BorrowResponse from(BorrowRequest r) {
        BorrowResponse dto = new BorrowResponse();
        dto.id = r.getId();
        dto.itemId = r.getItemId();
        dto.itemName = r.getItemName();
        dto.ownerId = r.getOwnerId();
        dto.borrowerId = r.getBorrowerId();
        dto.status = r.getStatus();
        dto.borrowDays = r.getBorrowDays();
        dto.note = r.getNote();
        dto.requestDate = r.getRequestDate();
        dto.decisionDate = r.getDecisionDate();
        dto.dueDate = r.getDueDate();
        dto.returnDate = r.getReturnDate();
        dto.lateFee = r.getLateFee();
        dto.securityDeposit = r.getSecurityDeposit();
        dto.overdue = r.getStatus() == BorrowStatus.ACCEPTED
                && r.getDueDate() != null
                && r.getDueDate().isBefore(LocalDateTime.now());
        return dto;
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public Long getOwnerId() { return ownerId; }
    public Long getBorrowerId() { return borrowerId; }
    public BorrowStatus getStatus() { return status; }
    public Integer getBorrowDays() { return borrowDays; }
    public String getNote() { return note; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public LocalDateTime getDecisionDate() { return decisionDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public Double getLateFee() { return lateFee; }
    public Double getSecurityDeposit() { return securityDeposit; }
    public boolean isOverdue() { return overdue; }
}
