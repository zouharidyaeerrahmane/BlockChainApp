package fr.inventory.controller;

import fr.inventory.model.Product;
import fr.inventory.model.Transaction;
import fr.inventory.service.ProductService;
import fr.inventory.service.TransactionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private Label lblTotalProducts;
    
    @FXML
    private Label lblActiveProducts;
    
    @FXML
    private Label lblLowStockProducts;
    
    @FXML
    private Label lblTotalTransactions;
    
    @FXML
    private Label lblPendingTransactions;
    
    @FXML
    private Label lblSyncedTransactions;
    
    @FXML
    private TableView<Product> tableLowStock;
    
    @FXML
    private TableColumn<Product, String> colProductName;
    
    @FXML
    private TableColumn<Product, Long> colCurrentStock;
    
    @FXML
    private TableColumn<Product, Long> colMinStock;
    
    @FXML
    private TableView<Transaction> tableRecentTransactions;
    
    @FXML
    private TableColumn<Transaction, String> colTransactionProduct;
    
    @FXML
    private TableColumn<Transaction, String> colTransactionType;
    
    @FXML
    private TableColumn<Transaction, Long> colTransactionQuantity;
    
    @FXML
    private TableColumn<Transaction, String> colTransactionDate;
    
    @FXML
    private ListView<String> listAlerts;

    private ProductService productService;
    private TransactionService transactionService;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTables();
    }

    private void setupTables() {
        // Low stock products table
        colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        
        // Recent transactions table
        colTransactionProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colTransactionType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedType()));
        colTransactionQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTransactionDate.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTimestamp().format(dateFormatter)));
    }

    public void setServices(ProductService productService, TransactionService transactionService) {
        this.productService = productService;
        this.transactionService = transactionService;
    }

    public void refreshData() {
        if (productService == null || transactionService == null) {
            return;
        }

        // Refresh in background thread
        Platform.runLater(() -> {
            try {
                refreshStatistics();
                refreshLowStockProducts();
                refreshRecentTransactions();
                refreshAlerts();
            } catch (Exception e) {
                System.err.println("Error refreshing dashboard data: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void refreshStatistics() {
        try {
            // Product statistics
            ProductService.ProductStats productStats = productService.getProductStats();
            lblTotalProducts.setText(String.valueOf(productStats.getTotalProducts()));
            lblActiveProducts.setText(String.valueOf(productStats.getActiveProducts()));
            lblLowStockProducts.setText(String.valueOf(productStats.getLowStockProducts()));
            
            // Transaction statistics
            TransactionService.TransactionStats transactionStats = transactionService.getTransactionStats();
            lblTotalTransactions.setText(String.valueOf(transactionStats.getTotalTransactions()));
            lblPendingTransactions.setText(String.valueOf(transactionStats.getPendingTransactions()));
            lblSyncedTransactions.setText(String.valueOf(transactionStats.getSyncedTransactions()));
            
        } catch (Exception e) {
            System.err.println("Error refreshing statistics: " + e.getMessage());
        }
    }

    private void refreshLowStockProducts() {
        try {
            List<Product> lowStockProducts = productService.getLowStockProducts();
            Platform.runLater(() -> {
                tableLowStock.getItems().clear();
                tableLowStock.getItems().addAll(lowStockProducts);
            });
        } catch (Exception e) {
            System.err.println("Error refreshing low stock products: " + e.getMessage());
        }
    }

    private void refreshRecentTransactions() {
        try {
            List<Transaction> recentTransactions = transactionService.getRecentTransactions(10);
            Platform.runLater(() -> {
                tableRecentTransactions.getItems().clear();
                tableRecentTransactions.getItems().addAll(recentTransactions);
            });
        } catch (Exception e) {
            System.err.println("Error refreshing recent transactions: " + e.getMessage());
        }
    }

    private void refreshAlerts() {
        try {
            Platform.runLater(() -> {
                listAlerts.getItems().clear();
                
                // Low stock alerts
                List<Product> lowStockProducts = productService.getLowStockProducts();
                for (Product product : lowStockProducts) {
                    listAlerts.getItems().add("⚠️ Stock faible: " + product.getName() + 
                        " (" + product.getCurrentStock() + "/" + product.getMinStock() + ")");
                }
                
                // Pending transactions alerts
                TransactionService.TransactionStats stats = transactionService.getTransactionStats();
                if (stats.getPendingTransactions() > 0) {
                    listAlerts.getItems().add("🔄 " + stats.getPendingTransactions() + 
                        " transaction(s) en attente de synchronisation blockchain");
                }
                
                // Blockchain connection status
                boolean blockchainConnected = transactionService.getBlockchainService().isConnected();
                if (!blockchainConnected) {
                    listAlerts.getItems().add("🔴 Connexion blockchain indisponible");
                } else {
                    listAlerts.getItems().add("🟢 Blockchain connectée");
                }
                
                // Add generic info if no alerts
                if (listAlerts.getItems().isEmpty()) {
                    listAlerts.getItems().add("✅ Aucune alerte - Système opérationnel");
                }
            });
        } catch (Exception e) {
            System.err.println("Error refreshing alerts: " + e.getMessage());
        }
    }

    @FXML
    private void refreshDashboard() {
        refreshData();
    }

    @FXML
    private void syncPendingTransactions() {
        if (transactionService == null) return;
        
        transactionService.syncPendingTransactions()
            .thenAccept(syncedCount -> {
                Platform.runLater(() -> {
                    if (syncedCount > 0) {
                        refreshStatistics();
                        refreshAlerts();
                        System.out.println("Synchronisé " + syncedCount + " transaction(s)");
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    System.err.println("Erreur lors de la synchronisation: " + throwable.getMessage());
                });
                return null;
            });
    }
}