package fr.inventory.dao;

import fr.inventory.model.Transaction;
import fr.inventory.model.TransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionDAO {
    
    /**
     * Create a new transaction
     * @param transaction the transaction to create
     * @return the created transaction with generated ID
     */
    Transaction create(Transaction transaction);
    
    /**
     * Find a transaction by ID
     * @param id the transaction ID
     * @return the transaction if found, empty optional otherwise
     */
    Optional<Transaction> findById(Long id);
    
    /**
     * Find all transactions
     * @return list of all transactions ordered by timestamp desc
     */
    List<Transaction> findAll();
    
    /**
     * Find transactions by product ID
     * @param productId the product ID
     * @return list of transactions for the product
     */
    List<Transaction> findByProductId(Long productId);
    
    /**
     * Find transactions by type
     * @param transactionType the transaction type (IN/OUT/TRANSFER)
     * @return list of transactions of the specified type
     */
    List<Transaction> findByType(TransactionType transactionType);
    
    /**
     * Find transactions by user
     * @param user the user name
     * @return list of transactions made by the user
     */
    List<Transaction> findByUser(String user);
    
    /**
     * Find transactions within a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of transactions within the date range
     */
    List<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find pending transactions (not synced to blockchain)
     * @return list of pending transactions
     */
    List<Transaction> findPending();
    
    /**
     * Find synced transactions (already on blockchain)
     * @return list of synced transactions
     */
    List<Transaction> findSynced();
    
    /**
     * Update transaction with blockchain hash
     * @param transactionId the transaction ID
     * @param blockchainTxHash the blockchain transaction hash
     * @return true if updated successfully
     */
    boolean updateBlockchainHash(Long transactionId, String blockchainTxHash);
    
    /**
     * Mark transaction as synced to blockchain
     * @param transactionId the transaction ID
     * @return true if updated successfully
     */
    boolean markAsSynced(Long transactionId);
    
    /**
     * Update an existing transaction
     * @param transaction the transaction to update
     * @return the updated transaction
     */
    Transaction update(Transaction transaction);
    
    /**
     * Delete a transaction (use with caution)
     * @param id the transaction ID to delete
     * @return true if deleted successfully
     */
    boolean delete(Long id);
    
    /**
     * Count total number of transactions
     * @return total count
     */
    long count();
    
    /**
     * Count transactions by type
     * @param transactionType the transaction type
     * @return count of transactions of the specified type
     */
    long countByType(TransactionType transactionType);
    
    /**
     * Count pending transactions
     * @return count of pending transactions
     */
    long countPending();
    
    /**
     * Get recent transactions (last N transactions)
     * @param limit number of transactions to retrieve
     * @return list of recent transactions
     */
    List<Transaction> findRecent(int limit);
}