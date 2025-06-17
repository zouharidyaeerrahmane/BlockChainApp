package fr.inventory.service;

import fr.inventory.blockchain.BlockchainService;
import fr.inventory.dao.ProductDAO;
import fr.inventory.dao.ProductDAOImpl;
import fr.inventory.dao.TransactionDAO;
import fr.inventory.dao.TransactionDAOImpl;
import fr.inventory.model.Product;
import fr.inventory.model.Transaction;
import fr.inventory.model.TransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TransactionService {
    private final TransactionDAO transactionDAO;
    private final ProductDAO productDAO;
    private final BlockchainService blockchainService;

    public TransactionService() {
        this.transactionDAO = new TransactionDAOImpl();
        this.productDAO = new ProductDAOImpl();
        this.blockchainService = new BlockchainService();
    }

    public TransactionService(TransactionDAO transactionDAO, ProductDAO productDAO, BlockchainService blockchainService) {
        this.transactionDAO = transactionDAO;
        this.productDAO = productDAO;
        this.blockchainService = blockchainService;
    }

    /**
     * Record a new transaction (IN/OUT/TRANSFER)
     */
    public CompletableFuture<Transaction> recordTransaction(Long productId, Long quantity, TransactionType type, String description, String user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input
                validateTransactionInput(productId, quantity, type, user);
                
                // Get product and verify it exists
                Optional<Product> productOpt = productDAO.findById(productId);
                if (productOpt.isEmpty()) {
                    throw new IllegalArgumentException("Product not found with ID: " + productId);
                }
                
                Product product = productOpt.get();
                if (!product.isActive()) {
                    throw new IllegalArgumentException("Cannot create transaction for inactive product");
                }
                
                // Check stock for OUT transactions
                if (type == TransactionType.OUT && product.getCurrentStock() < quantity) {
                    throw new IllegalArgumentException("Insufficient stock. Available: " + product.getCurrentStock() + ", Requested: " + quantity);
                }
                
                // Create transaction
                Transaction transaction = new Transaction(productId, quantity, type, description, user);
                Transaction savedTransaction = transactionDAO.create(transaction);
                
                // Update product stock
                updateProductStockForTransaction(product, quantity, type);
                
                // Record on blockchain asynchronously
                if (blockchainService.isConnected()) {
                    blockchainService.recordTransactionOnBlockchain(savedTransaction)
                        .thenAccept(txHash -> {
                            // Update transaction with blockchain hash
                            transactionDAO.updateBlockchainHash(savedTransaction.getId(), txHash);
                            transactionDAO.markAsSynced(savedTransaction.getId());
                            savedTransaction.setBlockchainTxHash(txHash);
                            savedTransaction.setSyncedToBlockchain(true);
                            System.out.println("Transaction recorded on blockchain with hash: " + txHash);
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Failed to record transaction on blockchain: " + throwable.getMessage());
                            return null;
                        });
                }
                
                return savedTransaction;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to record transaction: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Record stock input (IN transaction)
     */
    public CompletableFuture<Transaction> recordStockIn(Long productId, Long quantity, String description, String user) {
        return recordTransaction(productId, quantity, TransactionType.IN, description, user);
    }

    /**
     * Record stock output (OUT transaction)
     */
    public CompletableFuture<Transaction> recordStockOut(Long productId, Long quantity, String description, String user) {
        return recordTransaction(productId, quantity, TransactionType.OUT, description, user);
    }

    /**
     * Record stock transfer (TRANSFER transaction)
     */
    public CompletableFuture<Transaction> recordStockTransfer(Long productId, Long quantity, String description, String user) {
        return recordTransaction(productId, quantity, TransactionType.TRANSFER, description, user);
    }

    /**
     * Find transaction by ID
     */
    public Optional<Transaction> findById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return transactionDAO.findById(id);
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        return transactionDAO.findAll();
    }

    /**
     * Get transactions by product ID
     */
    public List<Transaction> getTransactionsByProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }
        return transactionDAO.findByProductId(productId);
    }

    /**
     * Get transactions by type
     */
    public List<Transaction> getTransactionsByType(TransactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        return transactionDAO.findByType(type);
    }

    /**
     * Get transactions by user
     */
    public List<Transaction> getTransactionsByUser(String user) {
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("User cannot be null or empty");
        }
        return transactionDAO.findByUser(user.trim());
    }

    /**
     * Get transactions within date range
     */
    public List<Transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        return transactionDAO.findByDateRange(startDate, endDate);
    }

    /**
     * Get pending transactions (not synced to blockchain)
     */
    public List<Transaction> getPendingTransactions() {
        return transactionDAO.findPending();
    }

    /**
     * Get synced transactions (already on blockchain)
     */
    public List<Transaction> getSyncedTransactions() {
        return transactionDAO.findSynced();
    }

    /**
     * Get recent transactions
     */
    public List<Transaction> getRecentTransactions(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        return transactionDAO.findRecent(limit);
    }

    /**
     * Sync pending transactions to blockchain
     */
    public CompletableFuture<Integer> syncPendingTransactions() {
        return CompletableFuture.supplyAsync(() -> {
            if (!blockchainService.isConnected()) {
                throw new RuntimeException("Blockchain service not connected");
            }
            
            List<Transaction> pendingTransactions = getPendingTransactions();
            int syncedCount = 0;
            
            for (Transaction transaction : pendingTransactions) {
                try {
                    String txHash = blockchainService.recordTransactionOnBlockchain(transaction).get();
                    transactionDAO.updateBlockchainHash(transaction.getId(), txHash);
                    transactionDAO.markAsSynced(transaction.getId());
                    syncedCount++;
                    System.out.println("Synced transaction " + transaction.getId() + " with hash: " + txHash);
                } catch (Exception e) {
                    System.err.println("Failed to sync transaction " + transaction.getId() + ": " + e.getMessage());
                }
            }
            
            return syncedCount;
        });
    }

    /**
     * Verify transaction on blockchain
     */
    public CompletableFuture<Boolean> verifyTransaction(Long transactionId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Transaction> transactionOpt = findById(transactionId);
            if (transactionOpt.isEmpty()) {
                return false;
            }
            
            Transaction transaction = transactionOpt.get();
            if (transaction.getBlockchainTxHash() == null) {
                return false;
            }
            
            try {
                return blockchainService.verifyTransaction(transaction.getBlockchainTxHash()).get();
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Get transaction statistics
     */
    public TransactionStats getTransactionStats() {
        long totalTransactions = transactionDAO.count();
        long pendingTransactions = transactionDAO.countPending();
        long inTransactions = transactionDAO.countByType(TransactionType.IN);
        long outTransactions = transactionDAO.countByType(TransactionType.OUT);
        long transferTransactions = transactionDAO.countByType(TransactionType.TRANSFER);
        
        return new TransactionStats(totalTransactions, pendingTransactions, 
                                  inTransactions, outTransactions, transferTransactions);
    }

    /**
     * Delete transaction (use with caution)
     */
    public boolean deleteTransaction(Long transactionId) {
        if (transactionId == null || transactionId <= 0) {
            throw new IllegalArgumentException("Invalid transaction ID");
        }
        return transactionDAO.delete(transactionId);
    }

    /**
     * Get blockchain service
     */
    public BlockchainService getBlockchainService() {
        return blockchainService;
    }

    // Private helper methods
    private void updateProductStockForTransaction(Product product, Long quantity, TransactionType type) {
        Long currentStock = product.getCurrentStock();
        Long newStock = currentStock;
        
        switch (type) {
            case IN:
                newStock = currentStock + quantity;
                break;
            case OUT:
                newStock = currentStock - quantity;
                break;
            case TRANSFER:
                // For transfers, stock level might remain the same or be handled differently
                // This depends on business logic
                break;
        }
        
        productDAO.updateStock(product.getId(), newStock);
    }

    private void validateTransactionInput(Long productId, Long quantity, TransactionType type, String user) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("User is required");
        }
        if (user.length() > 100) {
            throw new IllegalArgumentException("User name is too long (max 100 characters)");
        }
    }

    // Inner class for statistics
    public static class TransactionStats {
        private final long totalTransactions;
        private final long pendingTransactions;
        private final long inTransactions;
        private final long outTransactions;
        private final long transferTransactions;

        public TransactionStats(long totalTransactions, long pendingTransactions, 
                              long inTransactions, long outTransactions, long transferTransactions) {
            this.totalTransactions = totalTransactions;
            this.pendingTransactions = pendingTransactions;
            this.inTransactions = inTransactions;
            this.outTransactions = outTransactions;
            this.transferTransactions = transferTransactions;
        }

        public long getTotalTransactions() { return totalTransactions; }
        public long getPendingTransactions() { return pendingTransactions; }
        public long getSyncedTransactions() { return totalTransactions - pendingTransactions; }
        public long getInTransactions() { return inTransactions; }
        public long getOutTransactions() { return outTransactions; }
        public long getTransferTransactions() { return transferTransactions; }
    }
}