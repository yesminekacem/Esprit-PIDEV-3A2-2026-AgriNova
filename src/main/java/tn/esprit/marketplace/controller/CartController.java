package tn.esprit.marketplace.controller;

import tn.esprit.marketplace.service.CartService;
import tn.esprit.marketplace.service.OrderService;
import tn.esprit.marketplace.service.ProductListingService;
import tn.esprit.marketplace.entity.Cart;
import tn.esprit.marketplace.entity.Order;
import tn.esprit.marketplace.entity.ProductListing;
import javafx.scene.Parent;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CartController {

    private CartService cartService;
    private OrderService orderService;
    private ProductListingService productService;
    private String currentUser = "user2";

    @FXML private FlowPane cartGrid;
    @FXML private ScrollPane cartScrollPane;

    @FXML
    public void initialize() {
        cartService = new CartService();
        orderService = new OrderService();
        productService = new ProductListingService();
        loadCart();
    }

    private void loadCart() {
        try {
            List<Cart> cartItems = cartService.getCartByUser(currentUser);

            Platform.runLater(() -> {
                cartGrid.getChildren().clear();
                if (cartScrollPane != null) cartScrollPane.setVvalue(0.0);

                if (cartItems.isEmpty()) {
                    showEmptyCartMessage();
                    return;
                }

                double totalPrice = 0.0;

                for (Cart item : cartItems) {
                    // Ensure stock is correct
                    int realAvailableStock;
                    try {
                        ProductListing product = productService.getProductById(item.getProductId());
                        realAvailableStock = cartService.getAvailableStockForCart(item.getProductId(), currentUser);
                        item.setAvailableStock(realAvailableStock);
                        item.setPricePerUnit(product.getPrice_per_unit());
                        totalPrice += item.getQuantity() * product.getPrice_per_unit();
                    } catch (SQLException e) {
                        showAlert("Error", "Failed to fetch product: " + e.getMessage(), Alert.AlertType.ERROR);
                        continue;
                    }

                    HBox card = createCartItemCard(item);
                    cartGrid.getChildren().add(card);
                }

                addTotalRow(totalPrice, cartItems);
            });

        } catch (Exception e) {
            showAlert("Error", "Failed to load cart: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showEmptyCartMessage() {
        VBox emptyBox = new VBox(16);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(80));
        emptyBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 12; -fx-border-color: #cbd5e1; -fx-border-width: 2; -fx-border-radius: 12;");

        Label emptyLabel = new Label("🛒 Your cart is empty");
        emptyLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        // Add label to the VBox
        emptyBox.getChildren().add(emptyLabel);

        // Add VBox to the cart grid
        cartGrid.getChildren().add(emptyBox);
    }


    private HBox createCartItemCard(Cart item) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(24));
        card.setPrefWidth(950);
        card.setMaxWidth(950);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

        // Product image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(90);
        imageView.setFitHeight(90);
        imageView.setPreserveRatio(true);
        try {
            if (item.getPicture() != null) {
                File file = new File("src/main/resources/images/products/" + item.getPicture());
                if (file.exists()) imageView.setImage(new Image(file.toURI().toString()));
            }
        } catch (Exception ignored) {}

        VBox infoBox = new VBox(8);
        Label name = new Label(item.getProductName());
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label priceInfo = new Label(String.format("Price: %.2f TND / kg", item.getPricePerUnit()));
        priceInfo.setStyle("-fx-font-size: 15px; -fx-text-fill: #64748b;");
        Label qtyLabel = new Label("Qty: " + item.getQuantity() + " kg");
        qtyLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #64748b;");
        Label stockLabel = new Label("Available: " + item.getAvailableStock() + " kg");
        stockLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #059669;");

        infoBox.getChildren().addAll(name, priceInfo, qtyLabel, stockLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox actionsBox = new VBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Label totalLabel = new Label(String.format("%.2f TND", item.getPricePerUnit() * item.getQuantity()));
        totalLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #059669;");

        // Spinner
        int maxQty = item.getAvailableStock() + item.getQuantity();
        Spinner<Integer> spinner = new Spinner<>(1, maxQty, item.getQuantity());
        spinner.setEditable(true);
        spinner.setPrefWidth(100);

        // Error label
        Label errorLabel = new Label("❌ Exceeds available stock!");
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        Button updateBtn = new Button("💾 Update");
        updateBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 8; -fx-font-size: 13px;");

        // Spinner listener
        spinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int val = Integer.parseInt(newVal.trim());
                if (val <= 0 || val > maxQty) {
                    spinner.getEditor().setStyle("-fx-text-fill: red; -fx-border-color: #ef4444; -fx-border-width: 2;");
                    updateBtn.setDisable(true);
                    errorLabel.setVisible(true);
                } else {
                    spinner.getEditor().setStyle("-fx-text-fill: black; -fx-border-color: transparent;");
                    updateBtn.setDisable(false);
                    errorLabel.setVisible(false);
                    totalLabel.setText(String.format("%.2f TND", val * item.getPricePerUnit()));
                    qtyLabel.setText("Qty: " + val + " kg");
                }
            } catch (NumberFormatException ex) {
                updateBtn.setDisable(true);
                errorLabel.setVisible(true);
            }
        });

        updateBtn.setOnAction(e -> {
            spinner.commitValue();
            int desiredQty = spinner.getValue();
            int availableQty = item.getQuantity() + item.getAvailableStock();

            if (desiredQty > availableQty) {
                // Show error without refreshing the cart
                errorLabel.setVisible(true);
                spinner.getEditor().setStyle("-fx-text-fill: red; -fx-border-color: #ef4444; -fx-border-width: 2;");
                return; // DO NOT call loadCart()
            }

            try {
                cartService.updateCartQuantity(item.getId(), item.getProductId(), currentUser, desiredQty);
                loadCart(); // reload only if valid
            } catch (Exception ex) {
                showAlert("Error", "Failed to update: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        Button removeBtn = new Button("🗑️ Remove");
        removeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 8; -fx-font-size: 13px;");
        removeBtn.setOnAction(ev -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove " + item.getProductName() + " from cart?", ButtonType.OK, ButtonType.CANCEL);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    cartService.removeFromCart(item.getId());
                    loadCart();
                } catch (Exception ex) {
                    showAlert("Error", "Failed to remove: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        actionsBox.getChildren().addAll(totalLabel, spinner, errorLabel, updateBtn, removeBtn);
        card.getChildren().addAll(imageView, infoBox, spacer, actionsBox);
        return card;
    }







    private void addTotalRow(double total, List<Cart> cartItems) {
        Separator separator = new Separator();
        separator.setPrefHeight(2);
        cartGrid.getChildren().add(separator);

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_RIGHT);
        totalRow.setPadding(new Insets(20, 0, 20, 0));
        totalRow.setPrefWidth(950);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label totalLabel = new Label("Total: " + String.format("%.2f TND", total));
        totalLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button validateBtn = new Button("✅ Place Order");
        validateBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 14 28; -fx-background-radius: 12;");
        validateBtn.setOnAction(e -> handleValidateOrder(cartItems, total));

        totalRow.getChildren().addAll(spacer, totalLabel, validateBtn);
        cartGrid.getChildren().add(totalRow);
    }

    @FXML
    public void handleContinueShopping() {
        try {
            // Load the marketplace view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/marketplace/MarketplaceView.fxml"));
            Parent marketplaceView = loader.load();

            // Get the main layout controller
            Stage stage = (Stage) cartGrid.getScene().getWindow();
            Scene scene = stage.getScene();
            BorderPane mainLayout = (BorderPane) scene.getRoot(); // assuming root is a BorderPane
            mainLayout.setCenter(marketplaceView); // set marketplace in the center
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to navigate: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private void handleValidateOrder(List<Cart> cartItems, double total) {
        TextInputDialog addressDialog = new TextInputDialog();
        addressDialog.setTitle("Delivery Address");
        addressDialog.setHeaderText("Enter your delivery address:");
        addressDialog.setContentText("Street, City, ZIP");
        String address = addressDialog.showAndWait().orElse("").trim();
        if (address.isEmpty()) {
            showAlert("Cancelled", "Please enter a delivery address.", Alert.AlertType.WARNING);
            return;
        }

        ChoiceDialog<String> paymentDialog = new ChoiceDialog<>("Cash on Delivery", "Cash on Delivery", "Credit Card", "Bank Transfer");
        paymentDialog.setTitle("Payment Method");
        paymentDialog.setHeaderText("Choose payment method:");
        String payment = paymentDialog.showAndWait().orElse("").trim();
        if (payment.isEmpty()) {
            showAlert("Cancelled", "Please select a payment method.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Order order = new Order(currentUser, total, address, payment);
            orderService.createOrder(order, cartItems);
            cartService.clearCart(currentUser);
            showAlert("Success!", "Order placed successfully!\nTotal: " + String.format("%.2f TND", total), Alert.AlertType.INFORMATION);
            loadCart();
        } catch (SQLException e) {
            showAlert("Order Failed", "Failed to place order: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
