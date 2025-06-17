package fr.inventory.utils;

import java.sql.*;
import java.util.Properties;

public class DatabaseUtils {
    // private static final String H2_URL = "jdbc:h2:mem:inventory;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS INVENTORY";
    // private static final String H2_USER = "sa";
    //private static final String H2_PASSWORD = "";
    
    //MySQL configuration (commented for now, using H2 for simplicity)
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/inventory_db";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "";

    private static Connection connection;

    static {
        try {
            // Load H2 driver
            Class.forName("org.h2.Driver");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 database driver not found", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    private static void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection()) {
            createTables(conn);
            insertSampleData(conn);
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        // Create Products table
        String createProductsTable = """
            CREATE TABLE IF NOT EXISTS products (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                current_stock BIGINT NOT NULL DEFAULT 0,
                min_stock BIGINT NOT NULL DEFAULT 0,
                price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """;

        // Create Transactions table
        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                quantity BIGINT NOT NULL,
                transaction_type VARCHAR(20) NOT NULL,
                description TEXT,
                user_name VARCHAR(100),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                blockchain_tx_hash VARCHAR(255),
                synced_to_blockchain BOOLEAN NOT NULL DEFAULT FALSE,
                FOREIGN KEY (product_id) REFERENCES products(id)
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createProductsTable);
            stmt.execute(createTransactionsTable);
        }
    }

    private static void insertSampleData(Connection conn) throws SQLException {
        // Check if data already exists
        String checkData = "SELECT COUNT(*) FROM products";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkData)) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return; // Data already exists
            }
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    // Utility method to execute queries with parameters
    public static PreparedStatement prepareStatement(String sql) throws SQLException {
        return getConnection().prepareStatement(sql);
    }

    // Utility method to execute queries and return generated keys
    public static PreparedStatement prepareStatementWithGeneratedKeys(String sql) throws SQLException {
        return getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }
}