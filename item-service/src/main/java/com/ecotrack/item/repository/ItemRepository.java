package com.ecotrack.item.repository;

import com.ecotrack.item.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    List<Item> findByOwnerId(Long ownerId);
    
    List<Item> findByAvailable(Boolean available);

    Page<Item> findByAvailable(Boolean available, Pageable pageable);
    
    List<Item> findByCategory(String category);
    
    List<Item> findByNameContainingIgnoreCase(String name);
}
