package com.ecotrack.item.favorite;

import com.ecotrack.item.dto.ItemMapper;
import com.ecotrack.item.dto.ItemResponse;
import com.ecotrack.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wishlist / favorites. Identity comes from the trusted X-User-Id gateway header.
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    /** Full item objects the user has favorited. */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ItemResponse>> myFavorites(@RequestHeader("X-User-Id") Long userId) {
        List<Long> ids = favoriteRepository.findByUserId(userId).stream()
                .map(Favorite::getItemId).collect(Collectors.toList());
        List<ItemResponse> items = itemRepository.findAllById(ids).stream()
                .map(itemMapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    /** Just the favorited item IDs — handy for toggling heart icons. */
    @GetMapping("/ids")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Long>> myFavoriteIds(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(favoriteRepository.findByUserId(userId).stream()
                .map(Favorite::getItemId).collect(Collectors.toList()));
    }

    @PostMapping("/{itemId}")
    @Transactional
    public ResponseEntity<Map<String, Boolean>> add(@PathVariable Long itemId,
                                                     @RequestHeader("X-User-Id") Long userId) {
        if (!favoriteRepository.existsByUserIdAndItemId(userId, itemId)) {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setItemId(itemId);
            favoriteRepository.save(f);
        }
        return ResponseEntity.ok(Map.of("favorited", true));
    }

    @DeleteMapping("/{itemId}")
    @Transactional
    public ResponseEntity<Map<String, Boolean>> remove(@PathVariable Long itemId,
                                                       @RequestHeader("X-User-Id") Long userId) {
        favoriteRepository.deleteByUserIdAndItemId(userId, itemId);
        return ResponseEntity.ok(Map.of("favorited", false));
    }
}
