package fr.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    private Long id;
    private String name;
    private String description;
    private Long currentStock;
    private Long minStock;
    private BigDecimal price;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Product() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Product(String name, String description, Long currentStock, Long minStock, BigDecimal price) {
        this();
        this.name = name;
        this.description = description;
        this.currentStock = currentStock;
        this.minStock = minStock;
        this.price = price;
    }

    // Getters and Setters
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
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Long currentStock) {
        this.currentStock = currentStock;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getMinStock() {
        return minStock;
    }

    public void setMinStock(Long minStock) {
        this.minStock = minStock;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business logic methods
    public boolean isStockLow() {
        return currentStock != null && minStock != null && currentStock <= minStock;
    }

    public void updateStock(Long quantity, TransactionType type) {
        if (currentStock == null) {
            currentStock = 0L;
        }
        
        switch (type) {
            case IN:
                currentStock += quantity;
                break;
            case OUT:
                if (currentStock >= quantity) {
                    currentStock -= quantity;
                } else {
                    throw new IllegalArgumentException("Stock insuffisant pour cette opération");
                }
                break;
            case TRANSFER:
                // Transfer logic would be handled at service level
                break;
        }
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', currentStock=%d, minStock=%d, price=%s, isActive=%s}",
                id, name, currentStock, minStock, price, isActive);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}