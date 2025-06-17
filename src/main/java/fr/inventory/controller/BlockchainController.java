package fr.inventory.controller;

import fr.inventory.blockchain.BlockchainService;
import fr.inventory.service.ProductService;
import fr.inventory.service.TransactionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigInteger;
import java.net.URL;
import java.util.ResourceBundle;

public class BlockchainController implements Initializable {

    @FXML
    private Label lblConnectionStatus;
    
    @FXML
    private Label lblAccountAddress;
    
    @FXML
    private Label lblAccountBalance;
    
    @FXML
    private Label lblContractAddress;
    
    @FXML
    private TextArea txtContractInfo;
    
    @FXML
    private Button btnConnect;
    
    @FXML
    private Button btnDeployContract;
    
    @FXML
    private Button btnSyncTransactions;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    private TabPane tabPane;
    
    @FXML
    private Tab tabConnection;
    
    @FXML
    private Tab tabContract;
    
    @FXML
    private Tab tabSync;
    
    @FXML
    private TextArea txtSyncLog;
    
    @FXML
    private Label lblPendingTransactions;
    
    @FXML
    private Label lblSyncedTransactions;

    private ProductService productService;
    private TransactionService transactionService;
    private BlockchainService blockchainService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
    }

    private void setupUI() {
        progressIndicator.setVisible(false);
        btnDeployContract.setDisable(true);
        btnSyncTransactions.setDisable(true);
        
        // Setup sync log
        txtSyncLog.setEditable(false);
        txtSyncLog.setWrapText(true);
    }

    public void setServices(ProductService productService, TransactionService transactionService) {
        this.productService = productService;
        this.transactionService = transactionService;
        this.blockchainService = transactionService.getBlockchainService();
    }

    public void refreshBlockchainInfo() {
        if (blockchainService == null) return;
        
        Platform.runLater(() -> {
            updateConnectionStatus();
            updateAccountInfo();
            updateContractInfo();
            updateSyncInfo();
        });
    }

    private void updateConnectionStatus() {
        try {
            boolean connected = blockchainService.isConnected();
            
            if (connected) {
                lblConnectionStatus.setText("🟢 Connecté à Ganache");
                lblConnectionStatus.getStyleClass().removeAll("error", "warning");
                lblConnectionStatus.getStyleClass().add("success");
                btnConnect.setText("Rafraîchir");
                btnDeployContract.setDisable(false);
                btnSyncTransactions.setDisable(false);
            } else {
                lblConnectionStatus.setText("🔴 Déconnecté");
                lblConnectionStatus.getStyleClass().removeAll("success", "warning");
                lblConnectionStatus.getStyleClass().add("error");
                btnConnect.setText("Se connecter");
                btnDeployContract.setDisable(true);
                btnSyncTransactions.setDisable(true);
            }
        } catch (Exception e) {
            lblConnectionStatus.setText("❌ Erreur de connexion");
            lblConnectionStatus.getStyleClass().removeAll("success", "warning");
            lblConnectionStatus.getStyleClass().add("error");
            addToSyncLog("Erreur lors de la vérification de la connexion: " + e.getMessage());
        }
    }

    private void updateAccountInfo() {
        try {
            String accountAddress = blockchainService.getAccountAddress();
            if (accountAddress != null) {
                lblAccountAddress.setText(accountAddress);
                
                // Get account balance asynchronously
                blockchainService.getAccountBalance()
                    .thenAccept(balance -> {
                        Platform.runLater(() -> {
                            double etherBalance = balance.doubleValue() / Math.pow(10, 18);
                            lblAccountBalance.setText(String.format("%.4f ETH", etherBalance));
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> lblAccountBalance.setText("Erreur"));
                        return null;
                    });
            } else {
                lblAccountAddress.setText("Non disponible");
                lblAccountBalance.setText("Non disponible");
            }
        } catch (Exception e) {
            lblAccountAddress.setText("Erreur");
            lblAccountBalance.setText("Erreur");
            addToSyncLog("Erreur lors de la récupération des informations du compte: " + e.getMessage());
        }
    }

    private void updateContractInfo() {
        try {
            String contractAddress = blockchainService.getContractAddress();
            if (contractAddress != null && !contractAddress.equals("0x" + "0".repeat(40))) {
                lblContractAddress.setText(contractAddress);
                txtContractInfo.setText(
                    "Contrat déployé avec succès\n" +
                    "Adresse: " + contractAddress + "\n" +
                    "État: Opérationnel\n" +
                    "Fonctionnalités disponibles:\n" +
                    "- Ajout de produits\n" +
                    "- Enregistrement de transactions\n" +
                    "- Mise à jour des stocks\n" +
                    "- Traçabilité complète"
                );
            } else {
                lblContractAddress.setText("Non déployé");
                txtContractInfo.setText(
                    "Aucun contrat déployé\n\n" +
                    "Pour commencer à utiliser la blockchain:\n" +
                    "1. Assurez-vous que Ganache fonctionne\n" +
                    "2. Cliquez sur 'Déployer le contrat'\n" +
                    "3. Attendez la confirmation de déploiement\n" +
                    "4. Commencez à synchroniser les transactions"
                );
            }
        } catch (Exception e) {
            lblContractAddress.setText("Erreur");
            txtContractInfo.setText("Erreur lors de la récupération des informations du contrat: " + e.getMessage());
        }
    }

    private void updateSyncInfo() {
        if (transactionService == null) return;
        
        try {
            TransactionService.TransactionStats stats = transactionService.getTransactionStats();
            lblPendingTransactions.setText(String.valueOf(stats.getPendingTransactions()));
            lblSyncedTransactions.setText(String.valueOf(stats.getSyncedTransactions()));
        } catch (Exception e) {
            lblPendingTransactions.setText("Erreur");
            lblSyncedTransactions.setText("Erreur");
            addToSyncLog("Erreur lors de la récupération des statistiques: " + e.getMessage());
        }
    }

    @FXML
    private void connectToBlockchain() {
        progressIndicator.setVisible(true);
        addToSyncLog("Tentative de connexion à Ganache...");
        
        Platform.runLater(() -> {
            try {
                // The connection is handled in the BlockchainService constructor
                // Here we just refresh the status
                refreshBlockchainInfo();
                
                if (blockchainService.isConnected()) {
                    addToSyncLog("✅ Connexion établie avec succès");
                } else {
                    addToSyncLog("❌ Échec de la connexion - Vérifiez que Ganache fonctionne sur http://127.0.0.1:7545");
                }
            } catch (Exception e) {
                addToSyncLog("❌ Erreur de connexion: " + e.getMessage());
            } finally {
                progressIndicator.setVisible(false);
            }
        });
    }

    @FXML
    private void deployContract() {
        if (!blockchainService.isConnected()) {
            addToSyncLog("❌ Impossible de déployer - Aucune connexion blockchain");
            return;
        }

        progressIndicator.setVisible(true);
        btnDeployContract.setDisable(true);
        addToSyncLog("🚀 Déploiement du contrat en cours...");

        blockchainService.deployContract()
            .thenAccept(contractAddress -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnDeployContract.setDisable(false);
                    
                    if (contractAddress != null) {
                        addToSyncLog("✅ Contrat déployé avec succès à l'adresse: " + contractAddress);
                        refreshBlockchainInfo();
                    } else {
                        addToSyncLog("❌ Échec du déploiement du contrat");
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnDeployContract.setDisable(false);
                    addToSyncLog("❌ Erreur lors du déploiement: " + throwable.getMessage());
                });
                return null;
            });
    }

    @FXML
    private void syncTransactions() {
        if (!blockchainService.isConnected()) {
            addToSyncLog("❌ Impossible de synchroniser - Aucune connexion blockchain");
            return;
        }

        if (transactionService == null) {
            addToSyncLog("❌ Service de transactions non disponible");
            return;
        }

        progressIndicator.setVisible(true);
        btnSyncTransactions.setDisable(true);
        addToSyncLog("🔄 Synchronisation des transactions en cours...");

        transactionService.syncPendingTransactions()
            .thenAccept(syncedCount -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnSyncTransactions.setDisable(false);
                    
                    if (syncedCount > 0) {
                        addToSyncLog("✅ " + syncedCount + " transaction(s) synchronisée(s) avec succès");
                    } else {
                        addToSyncLog("ℹ️ Aucune transaction en attente de synchronisation");
                    }
                    
                    updateSyncInfo();
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnSyncTransactions.setDisable(false);
                    addToSyncLog("❌ Erreur lors de la synchronisation: " + throwable.getMessage());
                });
                return null;
            });
    }

    @FXML
    private void clearSyncLog() {
        txtSyncLog.clear();
        addToSyncLog("📝 Log effacé - " + java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }

    @FXML
    private void refreshAll() {
        refreshBlockchainInfo();
        addToSyncLog("🔄 Informations rafraîchies");
    }

    private void addToSyncLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtSyncLog.appendText("[" + timestamp + "] " + message + "\n");
            
            // Auto-scroll to bottom
            txtSyncLog.setScrollTop(Double.MAX_VALUE);
        });
    }

    @FXML
    private void showGanacheHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Configuration Ganache");
        alert.setHeaderText("Comment configurer Ganache");
        alert.setContentText(
            "Pour utiliser cette application avec Ganache:\n\n" +
            "1. Téléchargez et installez Ganache depuis https://trufflesuite.com/ganache/\n" +
            "2. Lancez Ganache et créez un nouveau workspace\n" +
            "3. Configurez le serveur RPC sur http://127.0.0.1:7545\n" +
            "4. Assurez-vous que le port 7545 est disponible\n" +
            "5. Cliquez sur 'Se connecter' dans cette application\n\n" +
            "Note: Ganache doit fonctionner en permanence pour que\n" +
            "les fonctionnalités blockchain soient disponibles."
        );
        alert.showAndWait();
    }
}