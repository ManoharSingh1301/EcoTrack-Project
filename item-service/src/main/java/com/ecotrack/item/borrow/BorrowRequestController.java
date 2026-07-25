package com.ecotrack.item.borrow;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Borrow-request workflow endpoints. Identity is taken from the trusted
 * X-User-Id header injected by the API gateway after JWT validation.
 */
@RestController
@RequestMapping("/api/borrow-requests")
@RequiredArgsConstructor
public class BorrowRequestController {

    private final BorrowRequestService service;

    @PostMapping
    public ResponseEntity<BorrowResponse> create(
            @Valid @RequestBody BorrowRequestDto dto,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequest(dto, userId));
    }

    /** Requests for items I own. */
    @GetMapping("/incoming")
    public ResponseEntity<List<BorrowResponse>> incoming(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.getIncoming(userId));
    }

    /** Requests I made to borrow. */
    @GetMapping("/outgoing")
    public ResponseEntity<List<BorrowResponse>> outgoing(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.getOutgoing(userId));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<BorrowResponse> accept(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.accept(id, userId));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BorrowResponse> reject(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.reject(id, userId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BorrowResponse> cancel(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.cancel(id, userId));
    }

    @PatchMapping("/{id}/return")
    public ResponseEntity<BorrowResponse> markReturned(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.markReturned(id, userId));
    }
}
