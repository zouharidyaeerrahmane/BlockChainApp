package fr.inventory.service;

import fr.inventory.blockchain.BlockchainService;
import fr.inventory.dao.ProductDAO;
import fr.inventory.dao.ProductDAOImpl;
import fr.inventory.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ProductService {
    private final ProductDAO productDAO;
    private final BlockchainService blockchainService;

    public ProductService() {
        this.productDAO = new ProductDAOImpl();
        this.blockchainService = new BlockchainService();
    }

    public ProductService(ProductDAO productDAO, BlockchainService blockchainService) {
        this.productDAO = productDAO;
        this.blockchainService = blockchainService;
    }

    /**
     * Create a new product and add it to blockchain
     */
    public CompletableFuture<Product> createProduct(String name, String description, Long initialStock, Long minStock, BigDecimal price) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input
                validateProductInput(name, description, initialStock, minStock, price);
                
                // Create product in database
                Product product = new Product(name, description, initialStock, minStock, price);
                Product savedProduct = productDAO.create(product);
                
                // Add to blockchain asynchronously
                if (blockchainService.isConnected()) {
                    blockchainService.addProductToBlockchain(savedProduct)
                        .thenAccept(txHash -> {
                            System.out.println("Product added to blockchain with hash: " + txHash);
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Failed to add product to blockchain: " + throwable.getMessage());
                            return null;
                        });
                }
                
                return savedProduct;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create product: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Find product by ID
     */
    public Optional<Product> findById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return productDAO.findById(id);
    }

    /**
     * Get all products
     */
    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    /**
     * Get all active products
     */
    public List<Product> getActiveProducts() {
        return productDAO.findAllActive();
    }

    /**
     * Get products with low stock
     */
    public List<Product> getLowStockProducts() {
        return productDAO.findLowStockProducts();
    }

    /**
     * Search products by name
     */
    public List<Product> searchProducts(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllProducts();
        }
        return productDAO.findByNameContaining(name.trim());
    }

    /**
     * Update product and sync to blockchain
     */
    public CompletableFuture<Product> updateProduct(Product product) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input
                validateProduct(product);
                
                // Update in database
                Product updatedProduct = productDAO.update(product);
                
                // Update stock on blockchain if connected
                if (blockchainService.isConnected()) {
                    blockchainService.updateProductStock(product.getId(), product.getCurrentStock())
                        .thenAccept(success -> {
                            if (success) {
                                System.out.println("Product stock updated on blockchain");
                            } else {
                                System.err.println("Failed to update product stock on blockchain");
                            }
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Failed to update product on blockchain: " + throwable.getMessage());
                            return null;
                        });
                }
                
                return updatedProduct;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Update product stock
     */
    public CompletableFuture<Boolean> updateStock(Long productId, Long newStock) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (productId == null || productId <= 0) {
                    throw new IllegalArgumentException("Invalid product ID");
                }
                if (newStock == null || newStock < 0) {
                    throw new IllegalArgumentException("Stock cannot be negative");
                }
                
                // Update in database
                boolean updated = productDAO.updateStock(productId, newStock);
                
                if (updated && blockchainService.isConnected()) {
                    // Update on blockchain
                    blockchainService.updateProductStock(productId, newStock)
                        .thenAccept(success -> {
                            if (success) {
                                System.out.println("Stock updated on blockchain for product ID: " + productId);
                            }
                        })
                        .exceptionally(throwable -> {
                            System.err.println("Failed to update stock on blockchain: " + throwable.getMessage());
                            return null;
                        });
                }
                
                return updated;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update stock: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Deactivate product
     */
    public boolean deactivateProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }
        return productDAO.deactivate(productId);
    }

    /**
     * Activate product
     */
    public boolean activateProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }
        return productDAO.activate(productId);
    }

    /**
     * Delete product (hard delete)
     */
    public boolean deleteProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }
        return productDAO.delete(productId);
    }

    /**
     * Get product statistics
     */
    public ProductStats getProductStats() {
        long totalProducts = productDAO.count();
        long activeProducts = productDAO.countActive();
        List<Product> lowStockProducts = getLowStockProducts();
        
        return new ProductStats(totalProducts, activeProducts, lowStockProducts.size());
    }

    /**
     * Check if product exists
     */
    public boolean productExists(Long productId) {
        return findById(productId).isPresent();
    }

    /**
     * Get blockchain service
     */
    public BlockchainService getBlockchainService() {
        return blockchainService;
    }

    // Validation methods
    private void validateProductInput(String name, String description, Long initialStock, Long minStock, BigDecimal price) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Product name is too long (max 255 characters)");
        }
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("Product description is too long (max 1000 characters)");
        }
        if (initialStock == null || initialStock < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative");
        }
        if (minStock == null || minStock < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (product.getId() == null || product.getId() <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }
        validateProductInput(product.getName(), product.getDescription(), 
                           product.getCurrentStock(), product.getMinStock(), product.getPrice());
    }

    // Inner class for statistics
    public static class ProductStats {
        private final long totalProducts;
        private final long activeProducts;
        private final long lowStockProducts;

        public ProductStats(long totalProducts, long activeProducts, long lowStockProducts) {
            this.totalProducts = totalProducts;
            this.activeProducts = activeProducts;
            this.lowStockProducts = lowStockProducts;
        }

        public long getTotalProducts() { return totalProducts; }
        public long getActiveProducts() { return activeProducts; }
        public long getLowStockProducts() { return lowStockProducts; }
        public long getInactiveProducts() { return totalProducts - activeProducts; }
    }
}