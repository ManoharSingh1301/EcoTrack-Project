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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Unit Tests")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Spy
    private ItemMapper itemMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private ItemRequest testItemRequest;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Power Drill");
        testItem.setDescription("A powerful cordless drill");
        testItem.setOwnerId(100L);
        testItem.setCategory("Tools");
        testItem.setAvailable(true);

        testItemRequest = ItemRequest.builder()
                .name("Power Drill")
                .description("A powerful cordless drill")
                .ownerId(100L)
                .category("Tools")
                .available(true)
                .build();
    }

    @Nested
    @DisplayName("getAllItems")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return all items as DTOs")
        void shouldReturnAllItemsAsDtos() {
            Item item2 = new Item();
            item2.setId(2L);
            item2.setName("Ladder");
            item2.setCategory("Equipment");
            item2.setAvailable(true);
            item2.setOwnerId(101L);

            when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem, item2));

            List<ItemResponse> result = itemService.getAllItems();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Power Drill");
            assertThat(result.get(1).getName()).isEqualTo("Ladder");
        }
    }

    @Nested
    @DisplayName("getItemById")
    class GetItemByIdTests {

        @Test
        @DisplayName("should return item when found")
        void shouldReturnItemWhenFound() {
            when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            Item result = itemService.getItemById(1L);

            assertThat(result.getName()).isEqualTo("Power Drill");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(itemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getItemById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Item not found");
        }
    }

    @Nested
    @DisplayName("createItem")
    class CreateItemTests {

        @Test
        @DisplayName("should create item successfully without image")
        void shouldCreateItemWithoutImage() {
            UserDto mockUser = UserDto.builder().id(100L).username("owner").build();
            when(userServiceClient.getUserById(100L)).thenReturn(mockUser);
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
                Item saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            ItemResponse result = itemService.createItem(testItemRequest, null);

            assertThat(result.getName()).isEqualTo("Power Drill");
            assertThat(result.getCategory()).isEqualTo("Tools");
            verify(itemRepository).save(any(Item.class));
        }

        @Test
        @DisplayName("should create item successfully with image")
        void shouldCreateItemWithImage() throws IOException {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getBytes()).thenReturn("image-data".getBytes());
            when(mockFile.getContentType()).thenReturn("image/png");
            when(mockFile.getOriginalFilename()).thenReturn("drill.png");

            UserDto mockUser = UserDto.builder().id(100L).username("owner").build();
            when(userServiceClient.getUserById(100L)).thenReturn(mockUser);
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
                Item saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            ItemResponse result = itemService.createItem(testItemRequest, mockFile);

            assertThat(result).isNotNull();
            verify(itemRepository).save(any(Item.class));
        }

        @Test
        @DisplayName("should throw BadRequestException when image processing fails")
        void shouldThrowWhenImageFails() throws IOException {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getBytes()).thenThrow(new IOException("File read error"));

            UserDto mockUser = UserDto.builder().id(100L).username("owner").build();
            when(userServiceClient.getUserById(100L)).thenReturn(mockUser);

            assertThatThrownBy(() -> itemService.createItem(testItemRequest, mockFile))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Failed to process image file");
        }
    }

    @Nested
    @DisplayName("deleteItem")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete item successfully")
        void shouldDeleteItemSuccessfully() {
            when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

            itemService.deleteItem(1L);

            verify(itemRepository).delete(testItem);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when item not found")
        void shouldThrowWhenItemNotFound() {
            when(itemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.deleteItem(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleAvailability")
    class ToggleAvailabilityTests {

        @Test
        @DisplayName("should toggle from available to unavailable")
        void shouldToggleToUnavailable() {
            testItem.setAvailable(true);
            when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
            when(itemRepository.save(any(Item.class))).thenReturn(testItem);

            ItemResponse result = itemService.toggleAvailability(1L);

            assertThat(testItem.getAvailable()).isFalse();
            verify(itemRepository).save(testItem);
        }

        @Test
        @DisplayName("should toggle from unavailable to available")
        void shouldToggleToAvailable() {
            testItem.setAvailable(false);
            when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
            when(itemRepository.save(any(Item.class))).thenReturn(testItem);

            ItemResponse result = itemService.toggleAvailability(1L);

            assertThat(testItem.getAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("getAvailableItems")
    class GetAvailableItemsTests {

        @Test
        @DisplayName("should return only available items")
        void shouldReturnOnlyAvailableItems() {
            when(itemRepository.findByAvailable(true)).thenReturn(List.of(testItem));

            List<ItemResponse> result = itemService.getAvailableItems();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("searchItemsByName")
    class SearchItemsTests {

        @Test
        @DisplayName("should find items matching search term")
        void shouldFindMatchingItems() {
            when(itemRepository.findByNameContainingIgnoreCase("drill"))
                    .thenReturn(List.of(testItem));

            List<ItemResponse> result = itemService.searchItemsByName("drill");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).containsIgnoringCase("drill");
        }
    }
}
