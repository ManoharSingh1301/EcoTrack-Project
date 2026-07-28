package com.ecotrack.item.borrow;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "borrow_requests", indexes = {
        @Index(name = "idx_borrow_owner", columnList = "owner_id"),
        @Index(name = "idx_borrow_borrower", columnList = "borrower_id"),
        @Index(name = "idx_borrow_item", columnList = "item_id"),
        @Index(name = "idx_borrow_status", columnList = "status")
})
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Snapshot of the item name at request time (avoids a cross-service lookup for display). */
    @Column(name = "item_name")
    private String itemName;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowStatus status = BorrowStatus.PENDING;

    /** Requested borrow duration in days. */
    @Column(name = "borrow_days", nullable = false)
    private Integer borrowDays;

    @Column(length = 500)
    private String note;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "decision_date")
    private LocalDateTime decisionDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    /** Computed on return: lateFeePerDay * days overdue. */
    @Column(name = "late_fee")
    private Double lateFee = 0.0;

    @Column(name = "security_deposit")
    private Double securityDeposit = 0.0;

    @PrePersist
    protected void onCreate() {
        if (requestDate == null) requestDate = LocalDateTime.now();
        if (status == null) status = BorrowStatus.PENDING;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }

    public BorrowStatus getStatus() { return status; }
    public void setStatus(BorrowStatus status) { this.status = status; }

    public Integer getBorrowDays() { return borrowDays; }
    public void setBorrowDays(Integer borrowDays) { this.borrowDays = borrowDays; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public LocalDateTime getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDateTime decisionDate) { this.decisionDate = decisionDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }

    public Double getLateFee() { return lateFee; }
    public void setLateFee(Double lateFee) { this.lateFee = lateFee; }

    public Double getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(Double securityDeposit) { this.securityDeposit = securityDeposit; }
}
