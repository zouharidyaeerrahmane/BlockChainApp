package fr.inventory.controller;

import fr.inventory.model.Product;
import fr.inventory.model.Transaction;
import fr.inventory.model.TransactionType;
import fr.inventory.service.ProductService;
import fr.inventory.service.TransactionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class TransactionController implements Initializable {

    @FXML
    private ComboBox<TransactionType> cmbTypeFilter;
    
    @FXML
    private ComboBox<String> cmbUserFilter;
    
    @FXML
    private CheckBox chkShowPendingOnly;
    
    @FXML
    private TableView<Transaction> tableTransactions;
    
    @FXML
    private TableColumn<Transaction, Long> colId;
    
    @FXML
    private TableColumn<Transaction, String> colProductName;
    
    @FXML
    private TableColumn<Transaction, Long> colQuantity;
    
    @FXML
    private TableColumn<Transaction, String> colType;
    
    @FXML
    private TableColumn<Transaction, String> colDescription;
    
    @FXML
    private TableColumn<Transaction, String> colUser;
    
    @FXML
    private TableColumn<Transaction, String> colTimestamp;
    
    @FXML
    private TableColumn<Transaction, String> colBlockchainStatus;
    
    @FXML
    private Button btnAddTransaction;
    
    @FXML
    private Button btnSyncToBlockchain;
    
    @FXML
    private Button btnVerifyTransaction;

    private ProductService productService;
    private TransactionService transactionService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        setupButtons();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedType()));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colTimestamp.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTimestamp().format(dateFormatter)));

        // Custom cell factory for blockchain status
        colBlockchainStatus.setCellValueFactory(cellData -> {
            Transaction transaction = cellData.getValue();
            String status;
            if (transaction.isSyncedToBlockchain()) {
                status = "✅ Synchronisé";
            } else {
                status = "⏳ En attente";
            }
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        colBlockchainStatus.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.contains("Synchronisé")) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("-fx-text-fill: orange;");
                    }
                }
            }
        });

        // Selection listener for buttons
        tableTransactions.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateButtonStates(newValue));
    }

    private void setupFilters() {
        // Type filter
        cmbTypeFilter.getItems().add(null); // All types
        cmbTypeFilter.getItems().addAll(TransactionType.values());
        cmbTypeFilter.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> filterTransactions());

        // User filter will be populated when transactions are loaded
        cmbUserFilter.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> filterTransactions());

        // Pending only filter
        chkShowPendingOnly.selectedProperty().addListener(
            (observable, oldValue, newValue) -> filterTransactions());
    }

    private void setupButtons() {
        btnVerifyTransaction.setDisable(true);
    }

    public void setServices(ProductService productService, TransactionService transactionService) {
        this.productService = productService;
        this.transactionService = transactionService;
    }

    public void refreshTransactions() {
        if (transactionService == null) return;
        
        Platform.runLater(() -> {
            try {
                List<Transaction> transactions = transactionService.getAllTransactions();
                tableTransactions.getItems().clear();
                tableTransactions.getItems().addAll(transactions);
                
                // Update user filter options
                updateUserFilter(transactions);
                
                // Apply current filters
                filterTransactions();
            } catch (Exception e) {
                showError("Erreur lors du chargement des transactions", e);
            }
        });
    }

    private void updateUserFilter(List<Transaction> transactions) {
        cmbUserFilter.getItems().clear();
        cmbUserFilter.getItems().add(null); // All users
        
        transactions.stream()
            .map(Transaction::getUser)
            .filter(user -> user != null && !user.trim().isEmpty())
            .distinct()
            .sorted()
            .forEach(cmbUserFilter.getItems()::add);
    }

    private void filterTransactions() {
        if (transactionService == null) return;
        
        try {
            List<Transaction> allTransactions = transactionService.getAllTransactions();
            List<Transaction> filteredTransactions = allTransactions.stream()
                .filter(transaction -> {
                    // Type filter
                    TransactionType selectedType = cmbTypeFilter.getSelectionModel().getSelectedItem();
                    if (selectedType != null && !transaction.getTransactionType().equals(selectedType)) {
                        return false;
                    }
                    
                    // User filter
                    String selectedUser = cmbUserFilter.getSelectionModel().getSelectedItem();
                    if (selectedUser != null && !selectedUser.equals(transaction.getUser())) {
                        return false;
                    }
                    
                    // Pending only filter
                    if (chkShowPendingOnly.isSelected() && transaction.isSyncedToBlockchain()) {
                        return false;
                    }
                    
                    return true;
                })
                .toList();
            
            Platform.runLater(() -> {
                tableTransactions.getItems().clear();
                tableTransactions.getItems().addAll(filteredTransactions);
            });
        } catch (Exception e) {
            showError("Erreur lors du filtrage", e);
        }
    }

    @FXML
    private void addTransaction() {
        showTransactionDialog();
    }

    @FXML
    private void syncToBlockchain() {
        if (transactionService == null) return;
        
        transactionService.syncPendingTransactions()
            .thenAccept(syncedCount -> {
                Platform.runLater(() -> {
                    refreshTransactions();
                    if (syncedCount > 0) {
                        showInfo("Synchronisé " + syncedCount + " transaction(s) vers la blockchain");
                    } else {
                        showInfo("Aucune transaction en attente de synchronisation");
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showError("Erreur lors de la synchronisation", (Exception) throwable);
                });
                return null;
            });
    }

    @FXML
    private void verifyTransaction() {
        Transaction selectedTransaction = tableTransactions.getSelectionModel().getSelectedItem();
        if (selectedTransaction == null || !selectedTransaction.isSyncedToBlockchain()) {
            return;
        }

        transactionService.verifyTransaction(selectedTransaction.getId())
            .thenAccept(verified -> {
                Platform.runLater(() -> {
                    if (verified) {
                        showInfo("Transaction vérifiée avec succès sur la blockchain");
                    } else {
                        showError("Impossible de vérifier la transaction sur la blockchain", null);
                    }
                });
            })
            .exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showError("Erreur lors de la vérification", (Exception) throwable);
                });
                return null;
            });
    }

    @FXML
    private void clearFilters() {
        cmbTypeFilter.getSelectionModel().clearSelection();
        cmbUserFilter.getSelectionModel().clearSelection();
        chkShowPendingOnly.setSelected(false);
        refreshTransactions();
    }

    private void updateButtonStates(Transaction selectedTransaction) {
        btnVerifyTransaction.setDisable(selectedTransaction == null || 
            !selectedTransaction.isSyncedToBlockchain());
    }

    private void showTransactionDialog() {
        if (productService == null) return;

        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Transaction");
        dialog.setHeaderText("Enregistrer une transaction d'inventaire");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<Product> productCombo = new ComboBox<>();
        ComboBox<TransactionType> typeCombo = new ComboBox<>();
        TextField quantityField = new TextField();
        TextField descriptionField = new TextField();
        TextField userField = new TextField();

        // Load products
        try {
            List<Product> activeProducts = productService.getActiveProducts();
            productCombo.getItems().addAll(activeProducts);
            productCombo.setCellFactory(listView -> new ListCell<Product>() {
                @Override
                protected void updateItem(Product product, boolean empty) {
                    super.updateItem(product, empty);
                    if (empty || product == null) {
                        setText(null);
                    } else {
                        setText(product.getName() + " (Stock: " + product.getCurrentStock() + ")");
                    }
                }
            });
            productCombo.setButtonCell(new ListCell<Product>() {
                @Override
                protected void updateItem(Product product, boolean empty) {
                    super.updateItem(product, empty);
                    if (empty || product == null) {
                        setText(null);
                    } else {
                        setText(product.getName() + " (Stock: " + product.getCurrentStock() + ")");
                    }
                }
            });
        } catch (Exception e) {
            showError("Erreur lors du chargement des produits", e);
            return;
        }

        typeCombo.getItems().addAll(TransactionType.values());
        userField.setText(System.getProperty("user.name"));

        grid.add(new Label("Produit:"), 0, 0);
        grid.add(productCombo, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Quantité:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descriptionField, 1, 3);
        grid.add(new Label("Utilisateur:"), 0, 4);
        grid.add(userField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Product selectedProduct = productCombo.getSelectionModel().getSelectedItem();
                    TransactionType selectedType = typeCombo.getSelectionModel().getSelectedItem();
                    Long quantity = Long.parseLong(quantityField.getText().trim());
                    String description = descriptionField.getText().trim();
                    String user = userField.getText().trim();

                    if (selectedProduct == null) {
                        showError("Veuillez sélectionner un produit", null);
                        return null;
                    }
                    if (selectedType == null) {
                        showError("Veuillez sélectionner un type de transaction", null);
                        return null;
                    }
                    if (quantity <= 0) {
                        showError("La quantité doit être positive", null);
                        return null;
                    }
                    if (user.isEmpty()) {
                        showError("L'utilisateur est obligatoire", null);
                        return null;
                    }

                    return new Transaction(selectedProduct.getId(), quantity, selectedType, description, user);
                } catch (NumberFormatException e) {
                    showError("Format de quantité invalide", e);
                    return null;
                }
            }
            return null;
        });

        Optional<Transaction> result = dialog.showAndWait();
        result.ifPresent(transactionData -> {
            try {
                transactionService.recordTransaction(
                    transactionData.getProductId(),
                    transactionData.getQuantity(),
                    transactionData.getTransactionType(),
                    transactionData.getDescription(),
                    transactionData.getUser()
                ).thenAccept(createdTransaction -> {
                    Platform.runLater(() -> {
                        refreshTransactions();
                        showInfo("Transaction enregistrée avec succès");
                    });
                }).exceptionally(throwable -> {
                    Platform.runLater(() -> showError("Erreur lors de l'enregistrement", (Exception) throwable));
                    return null;
                });
            } catch (Exception e) {
                showError("Erreur lors de l'opération", e);
            }
        });
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(message);
        alert.setContentText(e != null ? e.getMessage() : "Une erreur inattendue s'est produite.");
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}