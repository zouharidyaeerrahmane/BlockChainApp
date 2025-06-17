package fr.inventory.dao;

import fr.inventory.model.Transaction;
import fr.inventory.model.TransactionType;
import fr.inventory.utils.DatabaseUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionDAOImpl implements TransactionDAO {

    @Override
    public Transaction create(Transaction transaction) {
        String sql = """
            INSERT INTO transactions (product_id, quantity, transaction_type, description, user_name, blockchain_tx_hash, synced_to_blockchain) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatementWithGeneratedKeys(sql)) {
            stmt.setLong(1, transaction.getProductId());
            stmt.setLong(2, transaction.getQuantity());
            stmt.setString(3, transaction.getTransactionType().name());
            stmt.setString(4, transaction.getDescription());
            stmt.setString(5, transaction.getUser());
            stmt.setString(6, transaction.getBlockchainTxHash());
            stmt.setBoolean(7, transaction.isSyncedToBlockchain());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating transaction failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating transaction failed, no ID obtained.");
                }
            }
            
            return transaction;
        } catch (SQLException e) {
            throw new RuntimeException("Error creating transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            WHERE t.id = ?
        """;
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transaction by ID: " + e.getMessage(), e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<Transaction> findAll() {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            ORDER BY t.timestamp DESC
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all transactions: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    @Override
    public List<Transaction> findByProductId(Long productId) {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            WHERE t.product_id = ? 
            ORDER BY t.timestamp DESC
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by product ID: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    @Override
    public List<Transaction> findByType(TransactionType transactionType) {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            WHERE t.transaction_type = ? 
            ORDER BY t.timestamp DESC
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setString(1, transactionType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by type: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    @Override
    public List<Transaction> findByUser(String user) {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            WHERE t.user_name = ? 
            ORDER BY t.timestamp DESC
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setString(1, user);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by user: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    @Override
    public List<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            WHERE t.timestamp BETWEEN ? AND ? 
            ORDER BY t.timestamp DESC
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by date range: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    @Override
    public List<Transaction> findPending() {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            WHERE t.synced_to_blockchain = false 
            ORDER BY t.timestamp DESC
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding pending transactions: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    @Override
    public List<Transaction> findSynced() {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            WHERE t.synced_to_blockchain = true 
            ORDER BY t.timestamp DESC
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding synced transactions: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    @Override
    public boolean updateBlockchainHash(Long transactionId, String blockchainTxHash) {
        String sql = "UPDATE transactions SET blockchain_tx_hash = ? WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setString(1, blockchainTxHash);
            stmt.setLong(2, transactionId);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating blockchain hash: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean markAsSynced(Long transactionId) {
        String sql = "UPDATE transactions SET synced_to_blockchain = true WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, transactionId);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error marking transaction as synced: " + e.getMessage(), e);
        }
    }

    @Override
    public Transaction update(Transaction transaction) {
        String sql = """
            UPDATE transactions 
            SET product_id = ?, quantity = ?, transaction_type = ?, description = ?, user_name = ?, 
                blockchain_tx_hash = ?, synced_to_blockchain = ? 
            WHERE id = ?
        """;
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, transaction.getProductId());
            stmt.setLong(2, transaction.getQuantity());
            stmt.setString(3, transaction.getTransactionType().name());
            stmt.setString(4, transaction.getDescription());
            stmt.setString(5, transaction.getUser());
            stmt.setString(6, transaction.getBlockchainTxHash());
            stmt.setBoolean(7, transaction.isSyncedToBlockchain());
            stmt.setLong(8, transaction.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating transaction failed, no rows affected.");
            }
            
            return transaction;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM transactions";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting transactions: " + e.getMessage(), e);
        }
        
        return 0;
    }

    @Override
    public long countByType(TransactionType transactionType) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE transaction_type = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setString(1, transactionType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting transactions by type: " + e.getMessage(), e);
        }
        
        return 0;
    }

    @Override
    public long countPending() {
        String sql = "SELECT COUNT(*) FROM transactions WHERE synced_to_blockchain = false";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting pending transactions: " + e.getMessage(), e);
        }
        
        return 0;
    }

    @Override
    public List<Transaction> findRecent(int limit) {
        String sql = """
            SELECT t.*, p.name as product_name 
            FROM transactions t 
            LEFT JOIN products p ON t.product_id = p.id 
            ORDER BY t.timestamp DESC 
            LIMIT ?
        """;
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding recent transactions: " + e.getMessage(), e);
        }
        
        return transactions;
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        transaction.setProductId(rs.getLong("product_id"));
        transaction.setProductName(rs.getString("product_name"));
        transaction.setQuantity(rs.getLong("quantity"));
        transaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        transaction.setDescription(rs.getString("description"));
        transaction.setUser(rs.getString("user_name"));
        transaction.setBlockchainTxHash(rs.getString("blockchain_tx_hash"));
        transaction.setSyncedToBlockchain(rs.getBoolean("synced_to_blockchain"));
        
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            transaction.setTimestamp(timestamp.toLocalDateTime());
        }
        
        return transaction;
    }
}