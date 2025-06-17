package fr.inventory.dao;

import fr.inventory.model.Product;
import fr.inventory.utils.DatabaseUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAOImpl implements ProductDAO {

    @Override
    public Product create(Product product) {
        String sql = """
            INSERT INTO products (name, description, current_stock, min_stock, price, is_active) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatementWithGeneratedKeys(sql)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setLong(3, product.getCurrentStock());
            stmt.setLong(4, product.getMinStock());
            stmt.setBigDecimal(5, product.getPrice());
            stmt.setBoolean(6, product.isActive());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
            
            return product;
        } catch (SQLException e) {
            throw new RuntimeException("Error creating product: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Product> findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding product by ID: " + e.getMessage(), e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT * FROM products ORDER BY name";
        List<Product> products = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all products: " + e.getMessage(), e);
        }
        
        return products;
    }

    @Override
    public List<Product> findAllActive() {
        String sql = "SELECT * FROM products WHERE is_active = true ORDER BY name";
        List<Product> products = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding active products: " + e.getMessage(), e);
        }
        
        return products;
    }

    @Override
    public List<Product> findLowStockProducts() {
        String sql = "SELECT * FROM products WHERE current_stock <= min_stock AND is_active = true ORDER BY name";
        List<Product> products = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding low stock products: " + e.getMessage(), e);
        }
        
        return products;
    }

    @Override
    public List<Product> findByNameContaining(String name) {
        String sql = "SELECT * FROM products WHERE LOWER(name) LIKE LOWER(?) ORDER BY name";
        List<Product> products = new ArrayList<>();
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setString(1, "%" + name + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching products by name: " + e.getMessage(), e);
        }
        
        return products;
    }

    @Override
    public Product update(Product product) {
        String sql = """
            UPDATE products 
            SET name = ?, description = ?, current_stock = ?, min_stock = ?, price = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP 
            WHERE id = ?
        """;
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setLong(3, product.getCurrentStock());
            stmt.setLong(4, product.getMinStock());
            stmt.setBigDecimal(5, product.getPrice());
            stmt.setBoolean(6, product.isActive());
            stmt.setLong(7, product.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating product failed, no rows affected.");
            }
            
            return product;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStock(Long productId, Long newStock) {
        String sql = "UPDATE products SET current_stock = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, newStock);
            stmt.setLong(2, productId);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating product stock: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deactivate(Long id) {
        String sql = "UPDATE products SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deactivating product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean activate(Long id) {
        String sql = "UPDATE products SET is_active = true, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error activating product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting product: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM products";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting products: " + e.getMessage(), e);
        }
        
        return 0;
    }

    @Override
    public long countActive() {
        String sql = "SELECT COUNT(*) FROM products WHERE is_active = true";
        
        try (PreparedStatement stmt = DatabaseUtils.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting active products: " + e.getMessage(), e);
        }
        
        return 0;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setCurrentStock(rs.getLong("current_stock"));
        product.setMinStock(rs.getLong("min_stock"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            product.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            product.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return product;
    }
}