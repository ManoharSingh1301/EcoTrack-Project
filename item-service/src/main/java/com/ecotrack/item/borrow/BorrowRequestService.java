package com.ecotrack.item.borrow;

import com.ecotrack.item.exception.BadRequestException;
import com.ecotrack.item.exception.ResourceNotFoundException;
import com.ecotrack.item.exception.UnauthorizedActionException;
import com.ecotrack.item.model.Item;
import com.ecotrack.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Structured borrow-request workflow:
 * request → owner accepts/rejects → active borrow → return (with late-fee calc).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowRequestService {

    private final BorrowRequestRepository borrowRepo;
    private final ItemRepository itemRepository;

    @Transactional
    public BorrowResponse createRequest(BorrowRequestDto dto, Long borrowerId) {
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", dto.getItemId()));

        if (item.getOwnerId().equals(borrowerId)) {
            throw new BadRequestException("You cannot borrow your own item");
        }
        if (Boolean.FALSE.equals(item.getAvailable())) {
            throw new BadRequestException("This item is currently unavailable");
        }
        if (borrowRepo.existsByItemIdAndBorrowerIdAndStatus(item.getId(), borrowerId, BorrowStatus.PENDING)) {
            throw new BadRequestException("You already have a pending request for this item");
        }
        int maxDays = item.getMaxBorrowDays() != null ? item.getMaxBorrowDays() : 365;
        if (dto.getBorrowDays() > maxDays) {
            throw new BadRequestException("Owner allows a maximum of " + maxDays + " days for this item");
        }

        BorrowRequest r = new BorrowRequest();
        r.setItemId(item.getId());
        r.setItemName(item.getName());
        r.setOwnerId(item.getOwnerId());
        r.setBorrowerId(borrowerId);
        r.setStatus(BorrowStatus.PENDING);
        r.setBorrowDays(dto.getBorrowDays());
        r.setNote(dto.getNote());
        r.setSecurityDeposit(item.getSecurityDeposit() != null ? item.getSecurityDeposit() : 0.0);

        BorrowRequest saved = borrowRepo.save(r);
        log.info("Borrow request {} created: item {} by borrower {}", saved.getId(), item.getId(), borrowerId);
        return BorrowResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BorrowResponse> getIncoming(Long ownerId) {
        return borrowRepo.findByOwnerIdOrderByRequestDateDesc(ownerId).stream()
                .map(BorrowResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowResponse> getOutgoing(Long borrowerId) {
        return borrowRepo.findByBorrowerIdOrderByRequestDateDesc(borrowerId).stream()
                .map(BorrowResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public BorrowResponse accept(Long requestId, Long ownerId) {
        BorrowRequest r = requireOwner(requestId, ownerId);
        requireStatus(r, BorrowStatus.PENDING);

        Item item = itemRepository.findById(r.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", r.getItemId()));
        if (Boolean.FALSE.equals(item.getAvailable())) {
            throw new BadRequestException("Item is no longer available");
        }

        LocalDateTime now = LocalDateTime.now();
        r.setStatus(BorrowStatus.ACCEPTED);
        r.setDecisionDate(now);
        r.setDueDate(now.plusDays(r.getBorrowDays()));

        item.setAvailable(false);
        itemRepository.save(item);

        // Auto-reject other pending requests for the same item.
        borrowRepo.findByItemId(item.getId()).stream()
                .filter(other -> !other.getId().equals(r.getId()) && other.getStatus() == BorrowStatus.PENDING)
                .forEach(other -> {
                    other.setStatus(BorrowStatus.REJECTED);
                    other.setDecisionDate(now);
                    borrowRepo.save(other);
                });

        log.info("Borrow request {} accepted; item {} now unavailable, due {}", requestId, item.getId(), r.getDueDate());
        return BorrowResponse.from(borrowRepo.save(r));
    }

    @Transactional
    public BorrowResponse reject(Long requestId, Long ownerId) {
        BorrowRequest r = requireOwner(requestId, ownerId);
        requireStatus(r, BorrowStatus.PENDING);
        r.setStatus(BorrowStatus.REJECTED);
        r.setDecisionDate(LocalDateTime.now());
        return BorrowResponse.from(borrowRepo.save(r));
    }

    @Transactional
    public BorrowResponse cancel(Long requestId, Long borrowerId) {
        BorrowRequest r = borrowRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowRequest", "id", requestId));
        if (!r.getBorrowerId().equals(borrowerId)) {
            throw new UnauthorizedActionException("You can only cancel your own requests");
        }
        requireStatus(r, BorrowStatus.PENDING);
        r.setStatus(BorrowStatus.CANCELLED);
        r.setDecisionDate(LocalDateTime.now());
        return BorrowResponse.from(borrowRepo.save(r));
    }

    /** Either party may mark the item returned. Late fee is computed from the due date. */
    @Transactional
    public BorrowResponse markReturned(Long requestId, Long userId) {
        BorrowRequest r = borrowRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowRequest", "id", requestId));
        if (!r.getOwnerId().equals(userId) && !r.getBorrowerId().equals(userId)) {
            throw new UnauthorizedActionException("Only the owner or borrower can mark this returned");
        }
        requireStatus(r, BorrowStatus.ACCEPTED);

        LocalDateTime now = LocalDateTime.now();
        r.setStatus(BorrowStatus.RETURNED);
        r.setReturnDate(now);

        Item item = itemRepository.findById(r.getItemId()).orElse(null);
        double lateFeePerDay = item != null && item.getLateFeePerDay() != null ? item.getLateFeePerDay() : 0.0;
        if (r.getDueDate() != null && now.isAfter(r.getDueDate())) {
            long overdueDays = Math.max(1, Duration.between(r.getDueDate(), now).toDays());
            r.setLateFee(overdueDays * lateFeePerDay);
        } else {
            r.setLateFee(0.0);
        }

        if (item != null) {
            item.setAvailable(true);
            item.setBorrowCount((item.getBorrowCount() == null ? 0 : item.getBorrowCount()) + 1);
            itemRepository.save(item);
        }
        log.info("Borrow request {} returned; late fee {}", requestId, r.getLateFee());
        return BorrowResponse.from(borrowRepo.save(r));
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private BorrowRequest requireOwner(Long requestId, Long ownerId) {
        BorrowRequest r = borrowRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("BorrowRequest", "id", requestId));
        if (!r.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedActionException("Only the item owner can perform this action");
        }
        return r;
    }

    private void requireStatus(BorrowRequest r, BorrowStatus expected) {
        if (r.getStatus() != expected) {
            throw new BadRequestException("Action not allowed for a request in status " + r.getStatus());
        }
    }
}
