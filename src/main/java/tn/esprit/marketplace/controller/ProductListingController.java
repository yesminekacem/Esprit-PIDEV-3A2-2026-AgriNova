package tn.esprit.marketplace.controller;

import tn.esprit.marketplace.service.CartService;
import tn.esprit.marketplace.service.ProductListingService;
import tn.esprit.marketplace.entity.Cart;
import tn.esprit.marketplace.entity.ProductListing;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import java.io.IOException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class ProductListingController {

    private ProductListingService service;
    private CartService cartService;
    private ObservableList<ProductListing> productList;
    private String currentUser = "user2";

    // Marketplace fields
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private FlowPane productsGrid;
    @FXML private ScrollPane productsScrollPane;

    // Manage Products fields
    @FXML private FlowPane myProductsGrid;
    @FXML private ScrollPane myProductsScrollPane;
    @FXML private Label lblTotalProducts;
    @FXML private Label lblTotalCount;
    @FXML private Label lblAvailable;
    @FXML private Label lblAvailableCount;

    @FXML
    public void initialize() {
        service = new ProductListingService();
        cartService = new CartService();
        productList = FXCollections.observableArrayList();

        System.out.println("🎛️ ProductListingController initialize()");
        System.out.println("productsGrid: " + (productsGrid != null));
        System.out.println("myProductsGrid: " + (myProductsGrid != null));

        // Marketplace tab
        if (productsGrid != null) {
            System.out.println("🛒 Marketplace mode");
            cmbCategory.getItems().addAll("All", "Vegetables", "Grains", "Fruits");
            cmbCategory.setValue("All");
            loadProducts();
        }

        // Manage Products tab
        if (myProductsGrid != null) {
            System.out.println("📦 Manage Products mode");
            loadMyProducts();
        }
    }

    private void loadProducts() {
        try {
            productList.clear();

            // ⚠️ Load ALL products from OTHER users
            List<ProductListing> allProducts = service.getAllOtherUsersProducts(currentUser);
            productList.addAll(allProducts);

            long availableCount = allProducts.stream()
                    .filter(p -> "available".equalsIgnoreCase(p.getStatus()))
                    .count();

            System.out.println("🛒 Loaded " + allProducts.size() +
                    " total products (" + availableCount + " available)");

            updateStatsMarketplace(allProducts); // Update bottom labels

            Platform.runLater(() -> {
                displayProductCards();
                if (productsScrollPane != null) {
                    productsScrollPane.setVvalue(0.0);
                    productsScrollPane.requestLayout();
                }
            });

        } catch (SQLException e) {
            showAlert("Error", "Failed to load products: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private VBox createListingForm; // optional: a hidden VBox for the create listing form


    @FXML
    private void handleCreateListing() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Create New Product");

        // Buttons
        ButtonType createBtnType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);

        // Layout
        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 16;");

        // Fields
        TextField txtName = new TextField();
        txtName.setPromptText("Product Name");

        TextArea txtDescription = new TextArea();
        txtDescription.setPromptText("Product Description");
        txtDescription.setPrefRowCount(3);

        ComboBox<String> cmbCategory = new ComboBox<>();
        cmbCategory.getItems().addAll("Vegetables", "Grains", "Fruits");
        cmbCategory.setPromptText("Select Category");

        ComboBox<String> cmbStockStatus = new ComboBox<>();
        cmbStockStatus.getItems().addAll("Available", "Out of Stock");
        cmbStockStatus.setPromptText("Stock Status");

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("Price per kg (TND)");

        TextField txtQuantity = new TextField();
        txtQuantity.setPromptText("Quantity (kg)");

        TextField txtImagePath = new TextField();
        txtImagePath.setPromptText("No file selected");
        txtImagePath.setEditable(false);
        Button btnBrowse = new Button("Browse...");
        btnBrowse.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(myProductsGrid.getScene().getWindow());
            if (selectedFile != null) {
                txtImagePath.setText(selectedFile.getAbsolutePath());
            }
        });
        HBox imageBox = new HBox(8, txtImagePath, btnBrowse);

        content.getChildren().addAll(txtName, txtDescription, cmbCategory, cmbStockStatus, txtPrice, txtQuantity, imageBox);
        dialog.getDialogPane().setContent(content);

        // Handle Create button click
        Button createBtn = (Button) dialog.getDialogPane().lookupButton(createBtnType);
        createBtn.setOnAction(e -> {
            String name = txtName.getText().trim();
            String description = txtDescription.getText().trim();
            String category = cmbCategory.getValue();
            String stockStatus = cmbStockStatus.getValue();
            String imagePath = txtImagePath.getText();
            String priceStr = txtPrice.getText().trim();
            String quantityStr = txtQuantity.getText().trim();

            // --- INPUT VALIDATION ---
            if (name.isEmpty() || description.isEmpty() || category == null || stockStatus == null ||
                    priceStr.isEmpty() || quantityStr.isEmpty() || imagePath.isEmpty()) {
                showAlert("⚠️ Warning", "All fields are required!", Alert.AlertType.WARNING);
                e.consume();
                return;
            }

            // Name letters only
            if (!name.matches("[a-zA-Z ]+")) {
                showAlert("⚠️ Warning", "Product name must contain letters only.", Alert.AlertType.WARNING);
                e.consume();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int quantity = Integer.parseInt(quantityStr);

                if (price <= 0 || quantity <= 0) {
                    showAlert("⚠️ Warning", "Price and quantity must be greater than 0.", Alert.AlertType.WARNING);
                    e.consume();
                    return;
                }

                // Map stock status to DB ENUM
                String dbStatus;
                if ("Available".equalsIgnoreCase(stockStatus)) {
                    dbStatus = "available";   // matches ENUM
                } else if ("Out of Stock".equalsIgnoreCase(stockStatus)) {
                    dbStatus = "sold-out";    // matches ENUM
                } else {
                    dbStatus = "available";   // fallback
                }

                ProductListing newProduct = new ProductListing(
                        currentUser,
                        name,
                        price,
                        quantity,
                        dbStatus,                 // use mapped value
                        description,
                        new File(imagePath).getName(),
                        category
                );


                ProductListingService service = new ProductListingService();
                service.addMeth2(newProduct);

                showAlert("✅ Success", "Product created successfully!", Alert.AlertType.INFORMATION);

                loadMyProducts();
                loadProducts();
                dialog.close();

            } catch (NumberFormatException ex) {
                showAlert("⚠️ Warning", "Price and quantity must be valid numbers.", Alert.AlertType.WARNING);
                e.consume();
            } catch (Exception ex) {
                showAlert("❌ Error", "Failed to create product: " + ex.getMessage(), Alert.AlertType.ERROR);
                ex.printStackTrace();
            }
        });

        dialog.showAndWait();
    }







    private void updateStatsMarketplace(List<ProductListing> products) {
        if (lblTotalProducts != null) {
            lblTotalProducts.setText(String.valueOf(products.size()));
        }

        if (lblAvailable != null) {
            long available = products.stream()
                    .filter(p -> "available".equalsIgnoreCase(p.getStatus()))
                    .count();
            lblAvailable.setText(String.valueOf(available));
        }
    }



    private void loadMyProducts() {
        try {
            ProductListingService service = new ProductListingService();
            List<ProductListing> myProducts;

            if ("admin".equals(currentUser)) {
                myProducts = service.getAllProducts();
            } else {
                myProducts = service.getMyProducts(currentUser);
            }

            myProductsGrid.getChildren().clear();

            if (myProducts.isEmpty()) {
                Label emptyLabel = new Label("No products found.");
                emptyLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #475569;");
                myProductsGrid.getChildren().add(emptyLabel);
            } else {
                for (ProductListing product : myProducts) {
                    VBox card = createManageProductCard(product);
                    myProductsGrid.getChildren().add(card);
                }
            }

            // 🔹 UPDATE STATISTICS HERE
            if (lblTotalCount != null) {
                lblTotalCount.setText(String.valueOf(myProducts.size()));
            }
            if (lblAvailableCount != null) {
                long available = myProducts.stream()
                        .filter(p -> "available".equalsIgnoreCase(p.getStatus()))
                        .count();
                lblAvailableCount.setText(String.valueOf(available));
            }

            System.out.println("Manage Products loaded: " + myProducts.size());

        } catch (SQLException e) {
            showAlert("Error", "Failed to load products: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }


    @FXML
    private void handleSearch() {
        String searchTerm = txtSearch.getText().toLowerCase().trim();

        if (searchTerm.isEmpty()) {
            displayProductCards();
            return;
        }

        productsGrid.getChildren().clear();

        for (ProductListing product : productList) {
            if (product.getProduct_name().toLowerCase().contains(searchTerm)) {
                VBox card = createProductCard(product);
                productsGrid.getChildren().add(card);
            }
        }

        if (productsScrollPane != null) {
            productsScrollPane.setVvalue(0.0);
        }
    }

    private void displayProductCards() {
        Platform.runLater(() -> {
            if (productsGrid == null) return;

            productsGrid.getChildren().clear();

            for (ProductListing product : productList) {
                productsGrid.getChildren().add(createProductCard(product));
            }
        });
    }

    /**
     * 🔥 FIX #2 & #3:
     * - Status badge (Available / Out of Stock)
     * - Quantity formatted with String.format("%d kg")
     */
    private VBox createProductCard(ProductListing product) {
        VBox card = new VBox(12);
        card.setPrefWidth(360);
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

        ImageView img = new ImageView();
        img.setFitWidth(360);
        img.setFitHeight(200);
        loadProductImage(product, img);

        // Status Badge
        Label statusBadge = new Label();
        boolean isAvailable = "available".equalsIgnoreCase(product.getStatus());

        if (isAvailable) {
            statusBadge.setText("✅ Available");
            statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 6 12; -fx-background-radius: 12; -fx-font-weight: bold;");
        } else {
            statusBadge.setText("❌ Out of Stock");
            statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 6 12; -fx-background-radius: 12; -fx-font-weight: bold;");
        }

        Label name = new Label(product.getProduct_name());
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // 🔥 FIXED DASH ISSUE HERE
        Label priceQty = new Label(
                String.format("%.2f TND/kg  •  %s",
                        product.getPrice_per_unit(),
                        String.format("%d kg", product.getQuantity()))
        );

        Button addToCartBtn = new Button("🛒 Add to Cart");
        addToCartBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 12 24;");
        addToCartBtn.setMaxWidth(Double.MAX_VALUE);
        addToCartBtn.setDisable(!isAvailable);
        addToCartBtn.setOnAction(e -> handleAddToCart(product));

        VBox content = new VBox(10, statusBadge, name, priceQty, addToCartBtn);
        content.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(img, content);
        return card;
    }

    private VBox createManageProductCard(ProductListing product) {
        VBox card = new VBox(12);
        card.setPrefWidth(360);
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

        // Product image
        ImageView img = new ImageView();
        img.setFitWidth(360);
        img.setFitHeight(200);
        loadProductImage(product, img);

        // Name
        Label name = new Label(product.getProduct_name());
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Price + Quantity
        Label priceQty = new Label(
                String.format("Price: %.2f TND/kg  •  Stock: %d kg", product.getPrice_per_unit(), product.getQuantity())
        );

        // Action buttons
        HBox actions = new HBox(12);
        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 8 16;");
        editBtn.setOnAction(e -> handleUpdateProduct(product));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 8 16;");
        deleteBtn.setOnAction(e -> handleDeleteProduct(product));

        actions.getChildren().addAll(editBtn, deleteBtn);

        // Content VBox
        VBox content = new VBox(10, name, priceQty, actions);
        card.getChildren().addAll(img, content);

        return card;
    }


    private void loadProductImage(ProductListing product, ImageView imageView) {
        try {
            String path = "src/main/resources/images/products/" + product.getPicture();
            File file = new File(path);
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
        } catch (Exception e) {
            System.out.println("Image load failed: " + e.getMessage());
        }
    }

    private void handleAddToCart(ProductListing product) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🛒 Add to Cart - " + product.getProduct_name());

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));

        Label stockInfo = new Label(
                "📦 Available Stock: " + String.format("%d kg", product.getQuantity())
        );
        stockInfo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextField qtyField = new TextField("1");
        qtyField.setPromptText("Enter quantity (1-" + product.getQuantity() + ")");
        qtyField.setMaxWidth(200);

        Label errorLabel = new Label("");

        content.getChildren().addAll(stockInfo, qtyField, errorLabel);
        dialog.getDialogPane().setContent(content);

        ButtonType addBtn = new ButtonType("🛒 Add to Cart", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, cancelBtn);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(addBtn);
        okButton.setDisable(false);

        // 🔥 REAL-TIME VALIDATION
        qtyField.textProperty().addListener((obs, oldVal, newVal) -> {

            if (newVal.trim().isEmpty()) {
                errorLabel.setText("");
                okButton.setDisable(true);
                return;
            }

            try {
                int qty = Integer.parseInt(newVal.trim());

                if (qty <= 0) {
                    errorLabel.setText("❌ Quantity must be greater than 0");
                    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
                    okButton.setDisable(true);
                }
                else if (qty > product.getQuantity()) {
                    errorLabel.setText("❌ Cannot exceed available stock (" +
                            String.format("%d kg", product.getQuantity()) + ")");
                    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
                    okButton.setDisable(true);
                }
                else {
                    double total = qty * product.getPrice_per_unit();
                    errorLabel.setText("Total: " +
                            String.format("%.2f TND", total));
                    errorLabel.setStyle(""); // normal color (not red)
                    okButton.setDisable(false);
                }

            } catch (NumberFormatException e) {
                errorLabel.setText("❌ Please enter a valid number");
                errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
                okButton.setDisable(true);
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                try {
                    int quantity = Integer.parseInt(qtyField.getText().trim());

                    Cart cartItem = new Cart(
                            currentUser,
                            product.getListing_id(),
                            quantity
                    );

                    cartService.addToCart(cartItem);

                    showAlert("✅ Success",
                            "Added " + String.format("%d kg", quantity) +
                                    "\nTotal: " +
                                    String.format("%.2f TND",
                                            quantity * product.getPrice_per_unit()),
                            Alert.AlertType.INFORMATION);

                } catch (Exception e) {
                    showAlert("Error",
                            "Failed to add item: " + e.getMessage(),
                            Alert.AlertType.ERROR);
                }
            }
            return null;
        });

        dialog.showAndWait();
    }



    private void handleUpdateProduct(ProductListing product) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Product");

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 16;");

        // Fields pre-filled
        TextField txtName = new TextField(product.getProduct_name());
        TextArea txtDescription = new TextArea(product.getDescription());
        txtDescription.setPrefRowCount(3);

        ComboBox<String> cmbCategory = new ComboBox<>();
        cmbCategory.getItems().addAll("Vegetables", "Grains", "Fruits");
        cmbCategory.setValue(product.getCategory());

        ComboBox<String> cmbStockStatus = new ComboBox<>();
        cmbStockStatus.getItems().addAll("Available", "Out of Stock");
        cmbStockStatus.setValue(product.getStatus().equalsIgnoreCase("available") ? "Available" : "Out of Stock");

        TextField txtPrice = new TextField(String.valueOf(product.getPrice_per_unit()));
        TextField txtQuantity = new TextField(String.valueOf(product.getQuantity()));

        TextField txtImagePath = new TextField(product.getPicture());
        txtImagePath.setEditable(false);
        Button btnBrowse = new Button("Browse...");
        btnBrowse.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(myProductsGrid.getScene().getWindow());
            if (selectedFile != null) {
                txtImagePath.setText(selectedFile.getAbsolutePath());
            }
        });
        HBox imageBox = new HBox(8, txtImagePath, btnBrowse);

        content.getChildren().addAll(txtName, txtDescription, cmbCategory, cmbStockStatus, txtPrice, txtQuantity, imageBox);
        dialog.getDialogPane().setContent(content);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveBtn.setOnAction(e -> {
            String name = txtName.getText().trim();
            String description = txtDescription.getText().trim();
            String category = cmbCategory.getValue();
            String stockStatus = cmbStockStatus.getValue();
            String imagePath = txtImagePath.getText();
            String priceStr = txtPrice.getText().trim();
            String quantityStr = txtQuantity.getText().trim();

            // Validation
            if (name.isEmpty() || description.isEmpty() || category == null || stockStatus == null ||
                    priceStr.isEmpty() || quantityStr.isEmpty() || imagePath.isEmpty()) {
                showAlert("⚠️ Warning", "All fields are required!", Alert.AlertType.WARNING);
                e.consume();
                return;
            }

            if (!name.matches("[a-zA-Z ]+")) {
                showAlert("⚠️ Warning", "Product name must contain letters only.", Alert.AlertType.WARNING);
                e.consume();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                int quantity = Integer.parseInt(quantityStr);

                if (price <= 0 || quantity <= 0) {
                    showAlert("⚠️ Warning", "Price and quantity must be greater than 0.", Alert.AlertType.WARNING);
                    e.consume();
                    return;
                }

                // Update the product object
                product.setProduct_name(name);
                product.setDescription(description);
                product.setCategory(category);

                // Map stock status to DB ENUM
                String dbStatus;
                if ("Available".equalsIgnoreCase(cmbStockStatus.getValue())) {
                    dbStatus = "available";
                } else if ("Out of Stock".equalsIgnoreCase(cmbStockStatus.getValue())) {
                    dbStatus = "sold-out";
                } else {
                    dbStatus = "available";
                }
                product.setStatus(dbStatus);

                product.setPrice_per_unit(price);
                product.setQuantity(quantity);
                product.setPicture(new File(imagePath).getName());

                // Save to DB
                ProductListingService service = new ProductListingService();
                service.modifier(product);

                showAlert("✅ Success", "Product updated successfully!", Alert.AlertType.INFORMATION);

                // 🔹 Reload Manage Products (always)
                loadMyProducts();

                // 🔹 Reload Marketplace only if current user is NOT admin
                if (!"admin".equals(currentUser)) {
                    loadProducts();
                }

                dialog.close();

            } catch (NumberFormatException ex) {
                showAlert("⚠️ Warning", "Price and quantity must be valid numbers.", Alert.AlertType.WARNING);
                e.consume();
            } catch (Exception ex) {
                showAlert("❌ Error", "Failed to update product: " + ex.getMessage(), Alert.AlertType.ERROR);
                ex.printStackTrace();
            }
        });

        dialog.showAndWait();
    }




    private void handleDeleteProduct(ProductListing product) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Delete " + product.getProduct_name() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(product);
                loadMyProducts();
                loadProducts();
            } catch (Exception e) {
                showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void updateStats(List<ProductListing> products) {
        if (lblTotalCount != null) {
            lblTotalCount.setText(String.valueOf(products.size()));
        }

        if (lblAvailableCount != null) {
            long available = products.stream()
                    .filter(p -> "available".equalsIgnoreCase(p.getStatus()))
                    .count();
            lblAvailableCount.setText(String.valueOf(available));
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleShowAll() {
        txtSearch.clear();
        cmbCategory.setValue("All");
        loadProducts();
        System.out.println("🔄 Show all clicked");
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
        System.out.println("🔄 Refresh clicked");
    }
}
