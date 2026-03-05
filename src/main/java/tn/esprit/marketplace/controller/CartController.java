package tn.esprit.marketplace.controller;

import tn.esprit.marketplace.service.CartService;
import tn.esprit.marketplace.service.OrderService;
import tn.esprit.marketplace.service.ProductListingService;
import tn.esprit.marketplace.service.PayPalService;
import tn.esprit.marketplace.entity.Cart;
import tn.esprit.marketplace.entity.Order;
import tn.esprit.marketplace.entity.ProductListing;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.PayPalConfig;
import tn.esprit.user.entity.User;
import javafx.stage.Modality;
import javafx.scene.Parent;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
    private PayPalService payPalService;
    private User currentSessionUser;

    @FXML private FlowPane cartGrid;
    @FXML private ScrollPane cartScrollPane;

    @FXML
    public void initialize() {
        cartService = new CartService();
        orderService = new OrderService();
        productService = new ProductListingService();
        payPalService = new PayPalService();
        currentSessionUser = SessionManager.getInstance().getCurrentUser();
        loadCart();
    }

    private void loadCart() {
        try {
            List<Cart> cartItems = cartService.getCartByUser(String.valueOf(currentSessionUser.getId()));

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
                        realAvailableStock = cartService.getAvailableStockForCart(item.getProductId(), String.valueOf(currentSessionUser.getId()));
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
        emptyBox.getStyleClass().add("empty-cart-box");
        Label emptyLabel = new Label("🛒 Your cart is empty");
        emptyLabel.getStyleClass().add("empty-cart-label");

        emptyBox.getChildren().add(emptyLabel);
        cartGrid.getChildren().add(emptyBox);
    }



    private HBox createCartItemCard(Cart item) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(24));
        card.setPrefWidth(950);
        card.setMaxWidth(950);
        card.getStyleClass().add("cart-item-card");

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
        name.getStyleClass().add("cart-product-name");

        Label priceInfo = new Label(String.format("Price: %.2f TND / kg", item.getPricePerUnit()));
        priceInfo.getStyleClass().add("cart-price");

        Label qtyLabel = new Label("Qty: " + item.getQuantity() + " kg");
        qtyLabel.getStyleClass().add("cart-qty");

        Label stockLabel = new Label("Available: " + item.getAvailableStock() + " kg");
        stockLabel.getStyleClass().add("cart-stock");

        infoBox.getChildren().addAll(name, priceInfo, qtyLabel, stockLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox actionsBox = new VBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Label totalLabel = new Label(String.format("%.2f TND", item.getPricePerUnit() * item.getQuantity()));
        totalLabel.getStyleClass().add("cart-total-price");

        int maxQty = item.getAvailableStock() + item.getQuantity();
        Spinner<Integer> spinner = new Spinner<>(1, maxQty, item.getQuantity());
        spinner.setEditable(true);
        spinner.setPrefWidth(100);

        Label errorLabel = new Label("❌ Exceeds available stock!");
        errorLabel.getStyleClass().add("cart-error");
        errorLabel.setVisible(false);

        Button updateBtn = new Button("💾 Update");
        updateBtn.getStyleClass().add("cart-update-btn");

        spinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int val = Integer.parseInt(newVal.trim());
                if (val <= 0 || val > maxQty) {
                    spinner.getEditor().getStyleClass().add("invalid-spinner");
                    updateBtn.setDisable(true);
                    errorLabel.setVisible(true);
                } else {
                    spinner.getEditor().getStyleClass().remove("invalid-spinner");
                    spinner.getEditor().getStyleClass().add("valid-spinner");
                    updateBtn.setDisable(false);
                    errorLabel.setVisible(false);
                    totalLabel.setText(String.format("%.2f TND", val * item.getPricePerUnit()));
                    qtyLabel.setText("Qty: " + val + " kg");
                }
            } catch (NumberFormatException ex) {
                spinner.getEditor().getStyleClass().add("invalid-spinner");
                updateBtn.setDisable(true);
                errorLabel.setVisible(true);
            }
        });

        updateBtn.setOnAction(e -> {
            spinner.commitValue();
            int desiredQty = spinner.getValue();
            int availableQty = item.getQuantity() + item.getAvailableStock();

            if (desiredQty > availableQty) {
                errorLabel.setVisible(true);
                return;
            }

            try {
                cartService.updateCartQuantity(item.getId(), item.getProductId(), String.valueOf(currentSessionUser.getId()), desiredQty);
                loadCart();
            } catch (Exception ex) {
                showAlert("Error", "Failed to update: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        Button removeBtn = new Button("🗑️ Remove");
        removeBtn.getStyleClass().add("cart-remove-btn");

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
        totalRow.getStyleClass().add("cart-total-row");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label totalLabel = new Label("Total: " + String.format("%.2f TND", total));
        totalLabel.getStyleClass().add("cart-total-label");

        Button validateBtn = new Button("✅ Place Order");
        validateBtn.getStyleClass().add("cart-validate-btn");
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

        String address = null;
        Double lat = null;
        Double lng = null;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/fxml/marketplace/DeliveryMapDialog.fxml"
            ));
            Parent root = loader.load();

            DeliveryMapDialogController mapController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Select delivery location");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.getIcons().setAll(tn.esprit.MainFX.getAppIcon());

            dialogStage.showAndWait();

            if (!mapController.isConfirmed()) {
                showAlert("Cancelled", "Please select a delivery location.", Alert.AlertType.WARNING);
                return;
            }

            address = mapController.getSelectedAddress();
            lat = mapController.getSelectedLatitude();
            lng = mapController.getSelectedLongitude();

        } catch (IOException e) {
            showAlert("Error", "Failed to open map: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        ChoiceDialog<String> paymentDialog = new ChoiceDialog<>("Cash on Delivery",
                "Cash on Delivery", "Credit Card", "Paypal");
        paymentDialog.setTitle("Payment Method");
        paymentDialog.setHeaderText("Choose payment method:");
        String payment = paymentDialog.showAndWait().orElse("").trim();
        if (payment.isEmpty()) {
            showAlert("Cancelled", "Please select a payment method.", Alert.AlertType.WARNING);
            return;
        }

        // Handle PayPal payment
        if ("Paypal".equalsIgnoreCase(payment)) {
            handlePayPalPayment(cartItems, total, address, lat, lng);
        } else {
            // Handle other payment methods (Cash, Card, etc.)
            handleNonPayPalPayment(cartItems, total, address, lat, lng, payment);
        }
    }

    /**
     * Handle PayPal payment flow
     */
    private void handlePayPalPayment(List<Cart> cartItems, double total, String address, Double lat, Double lng) {
        // Show loading dialog while processing
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Processing");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Preparing PayPal checkout...");
        loadingAlert.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        loadingAlert.show();

        // Run PayPal setup in background thread
        Task<PayPalService.CreateOrderResult> setupTask = new Task<PayPalService.CreateOrderResult>() {
            @Override
            protected PayPalService.CreateOrderResult call() throws Exception {
                // Step 1: Get access token
                String accessToken = payPalService.getAccessToken();
                System.out.println("[Cart] Token obtained");

                // Step 2: Create order (convert TND to USD)
                double amountUSD = PayPalConfig.convertTNDtoUSD(total);
                System.out.println("[Cart] Converted " + total + " TND to " + amountUSD + " USD");
                return payPalService.createOrder(accessToken, PayPalConfig.PAYPAL_CURRENCY, amountUSD);
            }
        };

        setupTask.setOnSucceeded(e -> {
            loadingAlert.close();
            PayPalService.CreateOrderResult result = setupTask.getValue();

            // Step 3: Open PayPal approval dialog
            PayPalCheckoutDialog checkoutDialog = new PayPalCheckoutDialog();
            PayPalCheckoutDialog.CheckoutResult checkoutResult = checkoutDialog.showAndWait(result.approveUrl);

            if (checkoutResult == PayPalCheckoutDialog.CheckoutResult.APPROVED) {
                // Step 4: Capture the order
                handlePayPalCapture(result.orderId, cartItems, total, address, lat, lng);
            } else if (checkoutResult == PayPalCheckoutDialog.CheckoutResult.CANCELLED) {
                showAlert("Payment Cancelled", "You cancelled the PayPal payment.", Alert.AlertType.WARNING);
            } else {
                showAlert("Payment Error", "An error occurred during PayPal checkout.", Alert.AlertType.ERROR);
            }
        });

        setupTask.setOnFailed(e -> {
            loadingAlert.close();
            Throwable ex = setupTask.getException();
            showAlert("PayPal Error", "Failed to setup PayPal checkout: " + ex.getMessage(), Alert.AlertType.ERROR);
        });

        new Thread(setupTask).start();
    }

    /**
     * Capture PayPal order after user approval
     */
    private void handlePayPalCapture(String orderId, List<Cart> cartItems, double total, String address, Double lat, Double lng) {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Processing");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Finalizing your payment...");
        loadingAlert.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        loadingAlert.show();

        Task<String> captureTask = new Task<String>() {
            private String accessToken;

            @Override
            protected String call() throws Exception {
                // Get fresh access token for capture
                accessToken = payPalService.getAccessToken();
                // Capture the order
                return payPalService.captureOrder(accessToken, orderId);
            }
        };

        captureTask.setOnSucceeded(e -> {
            loadingAlert.close();
            String status = captureTask.getValue();

            if ("COMPLETED".equalsIgnoreCase(status)) {
                // Payment successful - create order in database
                try {
                    Order order = new Order(String.valueOf(currentSessionUser.getId()), total, address, lat, lng, "Paypal");
                    orderService.createOrder(order, cartItems);
                    cartService.clearCart(String.valueOf(currentSessionUser.getId()));

                    showAlert("Success!", "Payment completed!\nOrder placed successfully!\nTotal: " + String.format("%.2f TND", total),
                            Alert.AlertType.INFORMATION);
                    loadCart();
                } catch (SQLException ex) {
                    showAlert("Database Error", "Payment was successful but failed to save order: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Payment Failed", "PayPal payment status: " + status, Alert.AlertType.ERROR);
            }
        });

        captureTask.setOnFailed(e -> {
            loadingAlert.close();
            Throwable ex = captureTask.getException();
            showAlert("Capture Failed", "Failed to capture PayPal order: " + ex.getMessage(), Alert.AlertType.ERROR);
        });

        new Thread(captureTask).start();
    }

    /**
     * Handle non-PayPal payments (Cash, Card, etc.)
     */
    private void handleNonPayPalPayment(List<Cart> cartItems, double total, String address, Double lat, Double lng, String payment) {
        try {
            Order order = new Order(String.valueOf(currentSessionUser.getId()), total, address, lat, lng, payment);
            orderService.createOrder(order, cartItems);
            cartService.clearCart(String.valueOf(currentSessionUser.getId()));
            showAlert("Success!", "Order placed successfully!\nTotal: " + String.format("%.2f TND", total),
                    Alert.AlertType.INFORMATION);
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
