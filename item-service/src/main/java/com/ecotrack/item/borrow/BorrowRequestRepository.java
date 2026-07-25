package com.ecotrack.item.borrow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    /** Requests made against items I own (incoming). */
    List<BorrowRequest> findByOwnerIdOrderByRequestDateDesc(Long ownerId);

    /** Requests I made to borrow other people's items (outgoing). */
    List<BorrowRequest> findByBorrowerIdOrderByRequestDateDesc(Long borrowerId);

    List<BorrowRequest> findByItemId(Long itemId);

    boolean existsByItemIdAndBorrowerIdAndStatus(Long itemId, Long borrowerId, BorrowStatus status);

    long countByOwnerIdAndStatus(Long ownerId, BorrowStatus status);

    long countByBorrowerIdAndStatus(Long borrowerId, BorrowStatus status);
}
