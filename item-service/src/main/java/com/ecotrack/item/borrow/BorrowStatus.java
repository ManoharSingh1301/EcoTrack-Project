package com.ecotrack.item.borrow;

/**
 * Lifecycle of a borrow request.
 *
 * PENDING  → borrower has requested, awaiting owner decision
 * ACCEPTED → owner approved; item is actively borrowed until returned
 * REJECTED → owner declined the request
 * RETURNED → item handed back; transaction complete
 * CANCELLED→ borrower withdrew the request before it was accepted
 */
public enum BorrowStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    RETURNED,
    CANCELLED
}
