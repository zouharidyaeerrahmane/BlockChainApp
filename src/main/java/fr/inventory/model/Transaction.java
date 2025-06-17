package fr.inventory.model;

import java.time.LocalDateTime;

public class Transaction {
    private Long id;
    private Long productId;
    private String productName; // For display purposes
    private Long quantity;
    private TransactionType transactionType;
    private String description;
    private String user;
    private LocalDateTime timestamp;
    private String blockchainTxHash; // Hash of blockchain transaction
    private boolean syncedToBlockchain;

    // Constructors
    public Transaction() {
        this.timestamp = LocalDateTime.now();
        this.syncedToBlockchain = false;
    }

    public Transaction(Long productId, Long quantity, TransactionType transactionType, String description, String user) {
        this();
        this.productId = productId;
        this.quantity = quantity;
        this.transactionType = transactionType;
        this.description = description;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public boolean isSyncedToBlockchain() {
        return syncedToBlockchain;
    }

    public void setSyncedToBlockchain(boolean syncedToBlockchain) {
        this.syncedToBlockchain = syncedToBlockchain;
    }

    // Business logic methods
    public boolean isPending() {
        return !syncedToBlockchain;
    }

    public String getFormattedType() {
        return transactionType != null ? transactionType.getDisplayName() : "Unknown";
    }

    @Override
    public String toString() {
        return String.format("Transaction{id=%d, productId=%d, quantity=%d, type=%s, user='%s', timestamp=%s, synced=%s}",
                id, productId, quantity, transactionType, user, timestamp, syncedToBlockchain);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}