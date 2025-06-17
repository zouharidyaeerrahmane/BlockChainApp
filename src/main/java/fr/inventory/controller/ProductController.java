package fr.inventory.controller;

import fr.inventory.model.Product;
import fr.inventory.service.ProductService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProductController implements Initializable {

    @FXML
    private TextField txtSearch;
    
    @FXML
    private TableView<Product> tableProducts;
    
    @FXML
    private TableColumn<Product, Long> colId;
    
    @FXML
    private TableColumn<Product, String> colName;
    
    @FXML
    private TableColumn<Product, String> colDescription;
    
    @FXML
    private TableColumn<Product, Long> colCurrentStock;
    
    @FXML
    private TableColumn<Product, Long> colMinStock;
    
    @FXML
    private TableColumn<Product, BigDecimal> colPrice;
    
    @FXML
    private TableColumn<Product, Boolean> colActive;
    
    @FXML
    private TableColumn<Product, String> colCreatedAt;
    
    @FXML
    private Button btnAdd;
    
    @FXML
    private Button btnEdit;
    
    @FXML
    private Button btnDelete;
    
    @FXML
    private Button btnDeactivate;
    
    @FXML
    private Button btnActivate;
    
    @FXML
    private CheckBox chkShowInactive;

    private ProductService productService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupSearchListener();
        setupButtonStates();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCurrentStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        colCreatedAt.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null ? 
                cellData.getValue().getCreatedAt().format(dateFormatter) : ""));

        // Custom cell factory for stock column to highlight low stock
        colCurrentStock.setCellFactory(column -> new TableCell<Product, Long>() {
            @Override
            protected void updateItem(Long stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    Product product = getTableRow().getItem();
                    if (product != null && product.isStockLow()) {
                        setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Custom cell factory for active status
        colActive.setCellFactory(column -> new TableCell<Product, Boolean>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                } else {
                    setText(active ? "Actif" : "Inactif");
                    setStyle(active ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });

        // Selection listener for buttons
        tableProducts.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateButtonStates(newValue));
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            searchProducts(newValue);
        });
    }

    private void setupButtonStates() {
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        btnDeactivate.setDisable(true);
        btnActivate.setDisable(true);
    }

    public void setProductService(ProductService productService) {
        this.productService = productService;
    }

    public void refreshProducts() {
        if (productService == null) return;
        
        Platform.runLater(() -> {
            try {
                List<Product> products;
                if (chkShowInactive.isSelected()) {
                    products = productService.getAllProducts();
                } else {
                    products = productService.getActiveProducts();
                }
                
                tableProducts.getItems().clear();
                tableProducts.getItems().addAll(products);
            } catch (Exception e) {
                showError("Erreur lors du chargement des produits", e);
            }
        });
    }

    @FXML
    private void searchProducts(String searchText) {
        if (productService == null) return;
        
        Platform.runLater(() -> {
            try {
                List<Product> products;
                if (searchText == null || searchText.trim().isEmpty()) {
                    if (chkShowInactive.isSelected()) {
                        products = productService.getAllProducts();
                    } else {
                        products = productService.getActiveProducts();
                    }
                } else {
                    products = productService.searchProducts(searchText);
                    if (!chkShowInactive.isSelected()) {
                        products = products.stream()
                            .filter(Product::isActive).toList();
                    }
                }
                
                tableProducts.getItems().clear();
                tableProducts.getItems().addAll(products);
            } catch (Exception e) {
                showError("Erreur lors de la recherche", e);
            }
        });
    }

    @FXML
    private void addProduct() {
        showProductDialog(null);
    }

    @FXML
    private void editProduct() {
        Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            showProductDialog(selectedProduct);
        }
    }

    @FXML
    private void deleteProduct() {
        Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer le produit");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer le produit '" + 
            selectedProduct.getName() + "' ? Cette action est irréversible.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = productService.deleteProduct(selectedProduct.getId());
                    if (success) {
                        refreshProducts();
                        showInfo("Produit supprimé avec succès");
                    } else {
                        showError("Erreur lors de la suppression du produit", null);
                    }
                } catch (Exception e) {
                    showError("Erreur lors de la suppression", e);
                }
            }
        });
    }

    @FXML
    private void deactivateProduct() {
        Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) return;

        try {
            boolean success = productService.deactivateProduct(selectedProduct.getId());
            if (success) {
                refreshProducts();
                showInfo("Produit désactivé avec succès");
            } else {
                showError("Erreur lors de la désactivation du produit", null);
            }
        } catch (Exception e) {
            showError("Erreur lors de la désactivation", e);
        }
    }

    @FXML
    private void activateProduct() {
        Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) return;

        try {
            boolean success = productService.activateProduct(selectedProduct.getId());
            if (success) {
                refreshProducts();
                showInfo("Produit activé avec succès");
            } else {
                showError("Erreur lors de l'activation du produit", null);
            }
        } catch (Exception e) {
            showError("Erreur lors de l'activation", e);
        }
    }

    @FXML
    private void toggleShowInactive() {
        refreshProducts();
    }

    private void updateButtonStates(Product selectedProduct) {
        boolean hasSelection = selectedProduct != null;
        btnEdit.setDisable(!hasSelection);
        btnDelete.setDisable(!hasSelection);
        
        if (hasSelection) {
            btnDeactivate.setDisable(!selectedProduct.isActive());
            btnActivate.setDisable(selectedProduct.isActive());
        } else {
            btnDeactivate.setDisable(true);
            btnActivate.setDisable(true);
        }
    }

    private void showProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Ajouter un produit" : "Modifier le produit");
        dialog.setHeaderText(product == null ? "Créer un nouveau produit" : "Modifier les informations du produit");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField descriptionField = new TextField();
        TextField stockField = new TextField();
        TextField minStockField = new TextField();
        TextField priceField = new TextField();

        if (product != null) {
            nameField.setText(product.getName());
            descriptionField.setText(product.getDescription());
            stockField.setText(product.getCurrentStock().toString());
            minStockField.setText(product.getMinStock().toString());
            priceField.setText(product.getPrice().toString());
        }

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Stock actuel:"), 0, 2);
        grid.add(stockField, 1, 2);
        grid.add(new Label("Stock minimum:"), 0, 3);
        grid.add(minStockField, 1, 3);
        grid.add(new Label("Prix:"), 0, 4);
        grid.add(priceField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Validation
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText().trim();
                    String description = descriptionField.getText().trim();
                    Long stock = Long.parseLong(stockField.getText().trim());
                    Long minStock = Long.parseLong(minStockField.getText().trim());
                    BigDecimal price = new BigDecimal(priceField.getText().trim());

                    if (name.isEmpty()) {
                        showError("Le nom du produit est obligatoire", null);
                        return null;
                    }

                    if (product == null) {
                        // Create new product
                        return new Product(name, description, stock, minStock, price);
                    } else {
                        // Update existing product
                        product.setName(name);
                        product.setDescription(description);
                        product.setCurrentStock(stock);
                        product.setMinStock(minStock);
                        product.setPrice(price);
                        return product;
                    }
                } catch (NumberFormatException e) {
                    showError("Format de nombre invalide", e);
                    return null;
                }
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(productData -> {
            try {
                if (product == null) {
                    // Create new product
                    productService.createProduct(
                        productData.getName(),
                        productData.getDescription(),
                        productData.getCurrentStock(),
                        productData.getMinStock(),
                        productData.getPrice()
                    ).thenAccept(createdProduct -> {
                        Platform.runLater(() -> {
                            refreshProducts();
                            showInfo("Produit créé avec succès");
                        });
                    }).exceptionally(throwable -> {
                        Platform.runLater(() -> showError("Erreur lors de la création", (Exception) throwable));
                        return null;
                    });
                } else {
                    // Update existing product
                    productService.updateProduct(productData)
                        .thenAccept(updatedProduct -> {
                            Platform.runLater(() -> {
                                refreshProducts();
                                showInfo("Produit modifié avec succès");
                            });
                        }).exceptionally(throwable -> {
                            Platform.runLater(() -> showError("Erreur lors de la modification", (Exception) throwable));
                            return null;
                        });
                }
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