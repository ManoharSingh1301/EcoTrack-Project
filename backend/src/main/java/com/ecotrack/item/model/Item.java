package com.ecotrack.item.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "items", indexes = {
        @Index(name = "idx_items_owner_id", columnList = "owner_id"),
        @Index(name = "idx_items_category", columnList = "category"),
        @Index(name = "idx_items_available", columnList = "available"),
        @Index(name = "idx_items_name", columnList = "name")
})
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "Owner ID is required")
    @Column(nullable = false)
    private Long ownerId;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Boolean available = true;

    // 🔹 Borrowing metadata (enhancement)
    /** Physical condition: NEW, LIKE_NEW, GOOD, FAIR, WORN. */
    @Column(name = "item_condition")
    private String condition = "GOOD";

    /** Maximum number of days the item may be borrowed for. */
    @Column(name = "max_borrow_days")
    private Integer maxBorrowDays = 7;

    /** Fine charged per day when returned after the due date. */
    @Column(name = "late_fee_per_day")
    private Double lateFeePerDay = 0.0;

    /** Optional refundable security deposit. */
    @Column(name = "security_deposit")
    private Double securityDeposit = 0.0;

    /** How many times this item has been successfully borrowed. */
    @Column(name = "borrow_count")
    private Integer borrowCount = 0;

    // 🔹 Image Fields
    @Lob
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] imageData;

    @Column(name = "image_type")
    private String imageType;

    @Column(name = "image_name")
    private String imageName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 🔹 Default Constructor
    public Item() {}

    // 🔹 All Arguments Constructor
    public Item(Long id, String name, String description, Long ownerId, String category, Boolean available, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.category = category;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 🔹 Custom Constructor
    public Item(String name, String description, Long ownerId, String category, Boolean available) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.category = category;
        this.available = available;
    }

    // 🔹 Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Integer getMaxBorrowDays() {
        return maxBorrowDays;
    }

    public void setMaxBorrowDays(Integer maxBorrowDays) {
        this.maxBorrowDays = maxBorrowDays;
    }

    public Double getLateFeePerDay() {
        return lateFeePerDay;
    }

    public void setLateFeePerDay(Double lateFeePerDay) {
        this.lateFeePerDay = lateFeePerDay;
    }

    public Double getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(Double securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public Integer getBorrowCount() {
        return borrowCount;
    }

    public void setBorrowCount(Integer borrowCount) {
        this.borrowCount = borrowCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 🔹 Image Getters and Setters

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }

    // 🔹 Lifecycle Methods

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}