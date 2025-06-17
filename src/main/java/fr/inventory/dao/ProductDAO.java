package fr.inventory.dao;

import fr.inventory.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductDAO {
    
    /**
     * Create a new product
     * @param product the product to create
     * @return the created product with generated ID
     */
    Product create(Product product);
    
    /**
     * Find a product by ID
     * @param id the product ID
     * @return the product if found, empty optional otherwise
     */
    Optional<Product> findById(Long id);
    
    /**
     * Find all products
     * @return list of all products
     */
    List<Product> findAll();
    
    /**
     * Find all active products
     * @return list of active products
     */
    List<Product> findAllActive();
    
    /**
     * Find products with low stock
     * @return list of products with stock at or below minimum
     */
    List<Product> findLowStockProducts();
    
    /**
     * Search products by name
     * @param name the name to search for
     * @return list of matching products
     */
    List<Product> findByNameContaining(String name);
    
    /**
     * Update an existing product
     * @param product the product to update
     * @return the updated product
     */
    Product update(Product product);
    
    /**
     * Update product stock
     * @param productId the product ID
     * @param newStock the new stock quantity
     * @return true if updated successfully
     */
    boolean updateStock(Long productId, Long newStock);
    
    /**
     * Soft delete a product (set as inactive)
     * @param id the product ID to deactivate
     * @return true if deactivated successfully
     */
    boolean deactivate(Long id);
    
    /**
     * Activate a product
     * @param id the product ID to activate
     * @return true if activated successfully
     */
    boolean activate(Long id);
    
    /**
     * Hard delete a product (use with caution)
     * @param id the product ID to delete
     * @return true if deleted successfully
     */
    boolean delete(Long id);
    
    /**
     * Count total number of products
     * @return total count
     */
    long count();
    
    /**
     * Count active products
     * @return active products count
     */
    long countActive();
}