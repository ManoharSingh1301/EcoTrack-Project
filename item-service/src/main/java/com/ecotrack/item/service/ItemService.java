package com.ecotrack.item.service;

import com.ecotrack.item.client.UserDto;
import com.ecotrack.item.client.UserServiceClient;
import com.ecotrack.item.dto.ItemMapper;
import com.ecotrack.item.dto.ItemRequest;
import com.ecotrack.item.dto.ItemResponse;
import com.ecotrack.item.exception.BadRequestException;
import com.ecotrack.item.exception.ResourceNotFoundException;
import com.ecotrack.item.model.Item;
import com.ecotrack.item.repository.ItemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final UserServiceClient userServiceClient;

    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems() {
        log.debug("Fetching all items");
        return itemRepository.findAll().stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Item getItemById(Long id) {
        log.debug("Fetching item by id: {}", id);
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemResponseById(Long id) {
        return itemMapper.toResponse(getItemById(id));
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByOwnerId(Long ownerId) {
        log.debug("Fetching items for owner: {}", ownerId);
        return itemRepository.findByOwnerId(ownerId).stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "availableItems")
    @Transactional(readOnly = true)
    public List<ItemResponse> getAvailableItems() {
        log.debug("Fetching available items");
        return itemRepository.findByAvailable(true).stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "itemsByCategory", key = "#category")
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByCategory(String category) {
        log.debug("Fetching items by category: {}", category);
        return itemRepository.findByCategory(category).stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> searchItemsByName(String name) {
        log.debug("Searching items by name: {}", name);
        return itemRepository.findByNameContainingIgnoreCase(name).stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> getAvailableItemsPaginated(Pageable pageable) {
        log.debug("Fetching available items paginated: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        return itemRepository.findByAvailable(true, pageable)
                .map(itemMapper::toResponse);
    }

    /**
     * Verifies the owner exists via Feign call to user-service.
     * Circuit breaker protects against user-service failures.
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "verifyOwnerFallback")
    public UserDto verifyOwnerExists(Long ownerId) {
        log.info("Verifying owner exists via user-service: {}", ownerId);
        return userServiceClient.getUserById(ownerId);
    }

    public UserDto verifyOwnerFallback(Long ownerId, Exception ex) {
        log.warn("⚠️ User-service unavailable, allowing item creation without owner verification. OwnerId: {}", ownerId);
        return UserDto.builder().id(ownerId).username("Unverified").build();
    }

    @Caching(evict = {
            @CacheEvict(value = "availableItems", allEntries = true),
            @CacheEvict(value = "itemsByCategory", allEntries = true)
    })
    @Transactional
    public ItemResponse createItem(ItemRequest itemRequest, MultipartFile imageFile) {
        log.info("Creating new item: {} for owner: {}", itemRequest.getName(), itemRequest.getOwnerId());

        // Verify owner exists via Feign client
        verifyOwnerExists(itemRequest.getOwnerId());

        Item item = itemMapper.toEntity(itemRequest);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                item.setImageData(imageFile.getBytes());
                item.setImageType(imageFile.getContentType());
                item.setImageName(imageFile.getOriginalFilename());
            } catch (IOException e) {
                throw new BadRequestException("Failed to process image file", e);
            }
        }

        Item savedItem = itemRepository.save(item);
        log.info("Item created successfully with id: {}", savedItem.getId());
        return itemMapper.toResponse(savedItem);
    }

    @Caching(evict = {
            @CacheEvict(value = "availableItems", allEntries = true),
            @CacheEvict(value = "itemsByCategory", allEntries = true)
    })
    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest itemRequest, MultipartFile imageFile) {
        log.info("Updating item with id: {}", id);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));

        itemMapper.updateEntity(item, itemRequest);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                item.setImageData(imageFile.getBytes());
                item.setImageType(imageFile.getContentType());
                item.setImageName(imageFile.getOriginalFilename());
            } catch (IOException e) {
                throw new BadRequestException("Failed to process image file", e);
            }
        }

        Item updatedItem = itemRepository.save(item);
        log.info("Item updated successfully with id: {}", id);
        return itemMapper.toResponse(updatedItem);
    }

    @Caching(evict = {
            @CacheEvict(value = "availableItems", allEntries = true),
            @CacheEvict(value = "itemsByCategory", allEntries = true)
    })
    @Transactional
    public void deleteItem(Long id) {
        log.info("Deleting item with id: {}", id);
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));
        itemRepository.delete(item);
        log.info("Item deleted successfully with id: {}", id);
    }

    @Caching(evict = {
            @CacheEvict(value = "availableItems", allEntries = true)
    })
    @Transactional
    public ItemResponse toggleAvailability(Long id) {
        log.info("Toggling availability for item: {}", id);
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));
        item.setAvailable(!item.getAvailable());
        Item updatedItem = itemRepository.save(item);
        log.info("Item {} availability toggled to: {}", id, updatedItem.getAvailable());
        return itemMapper.toResponse(updatedItem);
    }
}
