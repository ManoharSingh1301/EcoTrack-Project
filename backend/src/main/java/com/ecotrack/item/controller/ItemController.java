package com.ecotrack.item.controller;

import com.ecotrack.item.dto.ItemRequest;
import com.ecotrack.item.dto.ItemResponse;
import com.ecotrack.common.exception.UnauthorizedActionException;
import com.ecotrack.item.model.Item;
import com.ecotrack.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // ── Read-only endpoints (public — no ownership check needed) ───────────────

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemResponseById(id));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ItemResponse>> getItemsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(itemService.getItemsByOwnerId(ownerId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<ItemResponse>> getAvailableItems() {
        return ResponseEntity.ok(itemService.getAvailableItems());
    }

    @GetMapping("/available/page")
    public ResponseEntity<Page<ItemResponse>> getAvailableItemsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(itemService.getAvailableItemsPaginated(pageable));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ItemResponse>> getItemsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(itemService.getItemsByCategory(category));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemResponse>> searchItems(@RequestParam String name) {
        return ResponseEntity.ok(itemService.searchItemsByName(name));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getItemImage(@PathVariable Long id) {
        Item item = itemService.getItemById(id);
        if (!item.hasImage() || item.getImageType() == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(item.getImageType()));
        headers.setContentLength(item.getImageData().length);
        return new ResponseEntity<>(item.getImageData(), headers, HttpStatus.OK);
    }

    // ── Write endpoints (require X-User-Id injected by gateway) ───────────────

    /**
     * Creates an item owned by the authenticated user.
     * The ownerId from the request body is ignored — identity comes from the JWT
     * via
     * the X-User-Id header injected by the API Gateway's JwtAuthenticationFilter.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponse> createItem(
            @Valid @RequestPart("item") ItemRequest itemRequest,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        // Enforce: the item owner is always the authenticated caller
        itemRequest.setOwnerId(authenticatedUserId);

        ItemResponse createdItem = itemService.createItem(itemRequest, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    /**
     * JSON-only variant of createItem — no image upload.
     * Accepts a plain {@code application/json} body so callers can POST without
     * wrapping the payload in multipart/form-data.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemResponse> createItemJson(
            @Valid @RequestBody ItemRequest itemRequest,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        itemRequest.setOwnerId(authenticatedUserId);

        ItemResponse createdItem = itemService.createItem(itemRequest, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    /**
     * Updates an item — only the owner may update their own item.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestPart("item") ItemRequest itemRequest,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        Item existing = itemService.getItemById(id);
        if (!existing.getOwnerId().equals(authenticatedUserId)) {
            throw new UnauthorizedActionException("You do not own this item");
        }

        ItemResponse updatedItem = itemService.updateItem(id, itemRequest, image);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * Toggles item availability — only the owner may change their own item.
     */
    @PatchMapping("/{id}/toggle-availability")
    public ResponseEntity<ItemResponse> toggleAvailability(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        Item existing = itemService.getItemById(id);
        if (!existing.getOwnerId().equals(authenticatedUserId)) {
            throw new UnauthorizedActionException("You do not own this item");
        }

        return ResponseEntity.ok(itemService.toggleAvailability(id));
    }

    /**
     * Deletes an item — only the owner may delete their own item.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        Item existing = itemService.getItemById(id);
        if (!existing.getOwnerId().equals(authenticatedUserId)) {
            throw new UnauthorizedActionException("You do not own this item");
        }

        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
