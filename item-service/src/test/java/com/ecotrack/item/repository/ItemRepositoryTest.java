package com.ecotrack.item.repository;

import com.ecotrack.item.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ItemRepository Integration Tests")
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();

        Item drill = new Item("Power Drill", "Cordless drill", 1L, "Tools", true);
        Item ladder = new Item("Ladder", "6ft aluminum ladder", 1L, "Equipment", true);
        Item saw = new Item("Circular Saw", "Electric saw", 2L, "Tools", false);
        Item mower = new Item("Lawn Mower", "Push mower", 2L, "Garden", true);

        itemRepository.saveAll(List.of(drill, ladder, saw, mower));
    }

    @Test
    @DisplayName("should find items by owner ID")
    void shouldFindByOwnerId() {
        List<Item> items = itemRepository.findByOwnerId(1L);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(Item::getName)
                .containsExactlyInAnyOrder("Power Drill", "Ladder");
    }

    @Test
    @DisplayName("should find available items")
    void shouldFindAvailableItems() {
        List<Item> items = itemRepository.findByAvailable(true);

        assertThat(items).hasSize(3);
        assertThat(items).allMatch(item -> item.getAvailable());
    }

    @Test
    @DisplayName("should find unavailable items")
    void shouldFindUnavailableItems() {
        List<Item> items = itemRepository.findByAvailable(false);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("Circular Saw");
    }

    @Test
    @DisplayName("should find items by category")
    void shouldFindByCategory() {
        List<Item> items = itemRepository.findByCategory("Tools");

        assertThat(items).hasSize(2);
        assertThat(items).extracting(Item::getCategory)
                .containsOnly("Tools");
    }

    @Test
    @DisplayName("should search items by name (case-insensitive)")
    void shouldSearchByNameCaseInsensitive() {
        List<Item> items = itemRepository.findByNameContainingIgnoreCase("drill");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("Power Drill");
    }

    @Test
    @DisplayName("should search items by partial name")
    void shouldSearchByPartialName() {
        List<Item> items = itemRepository.findByNameContainingIgnoreCase("ow");

        assertThat(items).hasSize(2); // Power Drill, Lawn Mower
    }

    @Test
    @DisplayName("should return paginated available items")
    void shouldReturnPaginatedItems() {
        Page<Item> page = itemRepository.findByAvailable(true, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("should persist createdAt timestamp")
    void shouldPersistCreatedAt() {
        Item item = itemRepository.findAll().get(0);

        assertThat(item.getCreatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
    }
}
