package fr.inventory.controller;

import fr.inventory.service.ProductService;
import fr.inventory.service.TransactionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    
    @FXML
    private BorderPane mainPane;
    
    @FXML
    private VBox sidebar;
    
    @FXML
    private Button btnDashboard;
    
    @FXML
    private Button btnProducts;
    
    @FXML
    private Button btnTransactions;
    
    @FXML
    private Button btnBlockchain;
    
    @FXML
    private Button btnReports;
    
    @FXML
    private Label lblConnectionStatus;
    
    @FXML
    private ProgressIndicator progressIndicator;

    private ProductService productService;
    private TransactionService transactionService;
    
    // Controllers for different views
    private DashboardController dashboardController;
    private ProductController productController;
    private TransactionController transactionController;
    private BlockchainController blockchainController;
    
    // Current active view
    private String currentView = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        productService = new ProductService();
        transactionService = new TransactionService();
        
        // Set up sidebar button actions
        setupSidebarActions();
        
        // Check blockchain connection status
        updateConnectionStatus();
        
        // Load dashboard by default
        showDashboard();
    }

    private void setupSidebarActions() {
        btnDashboard.setOnAction(e -> showDashboard());
        btnProducts.setOnAction(e -> showProducts());
        btnTransactions.setOnAction(e -> showTransactions());
        btnBlockchain.setOnAction(e -> showBlockchain());
    }

    @FXML
    private void showDashboard() {
        if ("dashboard".equals(currentView)) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent view = loader.load();
            
            dashboardController = loader.getController();
            dashboardController.setServices(productService, transactionService);
            dashboardController.refreshData();
            
            mainPane.setCenter(view);
            updateActiveButton(btnDashboard);
            currentView = "dashboard";
        } catch (IOException e) {
            showError("Erreur lors du chargement du tableau de bord", e);
        }
    }

    @FXML
    private void showProducts() {
        if ("products".equals(currentView)) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/products.fxml"));
            Parent view = loader.load();
            
            productController = loader.getController();
            productController.setProductService(productService);
            productController.refreshProducts();
            
            mainPane.setCenter(view);
            updateActiveButton(btnProducts);
            currentView = "products";
        } catch (IOException e) {
            showError("Erreur lors du chargement de la gestion des produits", e);
        }
    }

    @FXML
    private void showTransactions() {
        if ("transactions".equals(currentView)) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/transactions.fxml"));
            Parent view = loader.load();
            
            transactionController = loader.getController();
            transactionController.setServices(productService, transactionService);
            transactionController.refreshTransactions();
            
            mainPane.setCenter(view);
            updateActiveButton(btnTransactions);
            currentView = "transactions";
        } catch (IOException e) {
            showError("Erreur lors du chargement de la gestion des transactions", e);
        }
    }

    @FXML
    private void showBlockchain() {
        if ("blockchain".equals(currentView)) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/blockchain.fxml"));
            Parent view = loader.load();
            
            blockchainController = loader.getController();
            blockchainController.setServices(productService, transactionService);
            blockchainController.refreshBlockchainInfo();
            
            mainPane.setCenter(view);
            updateActiveButton(btnBlockchain);
            currentView = "blockchain";
        } catch (IOException e) {
            showError("Erreur lors du chargement de la vue blockchain", e);
        }
    }

    @FXML


    private void updateActiveButton(Button activeButton) {
        // Remove active class from all buttons
        sidebar.getChildren().stream()
            .filter(node -> node instanceof Button)
            .map(node -> (Button) node)
            .forEach(button -> button.getStyleClass().remove("active"));
        
        // Add active class to current button
        activeButton.getStyleClass().add("active");
    }

    private void updateConnectionStatus() {
        progressIndicator.setVisible(true);
        
        // Check blockchain connection in background
        Platform.runLater(() -> {
            try {
                boolean connected = transactionService.getBlockchainService().isConnected();
                
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    if (connected) {
                        lblConnectionStatus.setText("Connecté à Ganache");
                        lblConnectionStatus.getStyleClass().removeAll("error", "warning");
                        lblConnectionStatus.getStyleClass().add("success");
                    } else {
                        lblConnectionStatus.setText("Déconnecté de Ganache");
                        lblConnectionStatus.getStyleClass().removeAll("success", "warning");
                        lblConnectionStatus.getStyleClass().add("error");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    lblConnectionStatus.setText("Erreur de connexion");
                    lblConnectionStatus.getStyleClass().removeAll("success", "warning");
                    lblConnectionStatus.getStyleClass().add("error");
                });
            }
        });
    }

    @FXML
    private void refreshConnectionStatus() {
        updateConnectionStatus();
        
        // Also refresh current view if it's blockchain-related
        if ("blockchain".equals(currentView) && blockchainController != null) {
            blockchainController.refreshBlockchainInfo();
        }
    }

    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("Système de Gestion d'Inventaire avec Blockchain");
        alert.setContentText(
            "Version: 1.0.0\n" +
            "Développé avec JavaFX et Web3j\n" +
            "Intégration Blockchain avec Ganache\n\n" +
            "Ce système permet la gestion sécurisée et transparente\n" +
            "de l'inventaire en utilisant la technologie blockchain."
        );
        alert.showAndWait();
    }

    @FXML
    private void exitApplication() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Fermer l'application");
        alert.setContentText("Êtes-vous sûr de vouloir quitter l'application ?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Cleanup blockchain service
                if (transactionService != null) {
                    transactionService.getBlockchainService().shutdown();
                }
                Platform.exit();
                System.exit(0);
            }
        });
    }

    private void showError(String message, Exception e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(message);
            alert.setContentText(e != null ? e.getMessage() : "Une erreur inattendue s'est produite.");
            alert.showAndWait();
        });
    }

    // Getters for services (useful for testing)
    public ProductService getProductService() {
        return productService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }
}