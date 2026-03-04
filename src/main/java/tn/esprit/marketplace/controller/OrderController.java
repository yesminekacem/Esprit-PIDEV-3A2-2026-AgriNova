package tn.esprit.marketplace.controller;

import tn.esprit.marketplace.service.OrderService;
import tn.esprit.marketplace.entity.Order;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import tn.esprit.utils.SessionManager;

public class OrderController {
    private OrderService orderService;

    @FXML private FlowPane ordersGrid;

    @FXML
    public void initialize() {
        orderService = new OrderService();

        if (isAdmin()) {
            loadOrders();
        } else {
            loadUserOrders();
        }
    }


    private String getCurrentUserId() {
        return String.valueOf(SessionManager.getInstance().getCurrentUser().getId());
    }

    private boolean isAdmin() {
        return SessionManager.getInstance().isAdmin();
    }

    private void loadOrders() {
        try {
            ordersGrid.getChildren().clear();
            List<Order> orders = orderService.getAllOrders();

            if (orders.isEmpty()) {
                Label emptyLabel = new Label("📦 No orders yet");
                emptyLabel.getStyleClass().add("page-subtitle");
                ordersGrid.getChildren().add(emptyLabel);
                return;
            }

            for (Order order : orders) {
                VBox card = createOrderCard(order);
                ordersGrid.getChildren().add(card);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load orders: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadUserOrders() {
        try {
            ordersGrid.getChildren().clear();
            List<Order> orders = orderService.getOrdersByUser(getCurrentUserId());

            if (orders.isEmpty()) {
                Label emptyLabel = new Label("📦 No orders yet");
                emptyLabel.getStyleClass().add("page-subtitle");
                ordersGrid.getChildren().add(emptyLabel);
                return;
            }

            for (Order order : orders) {
                VBox card = createUserOrderCard(order);
                ordersGrid.getChildren().add(card);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load your orders: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("product-card");
        card.setPrefWidth(360);

        Label orderInfo = new Label("Order #" + order.getId());
        orderInfo.getStyleClass().add("product-title");

        Label customerInfo = new Label("Customer: " + order.getUserId());
        customerInfo.getStyleClass().add("stock-label");

        Label totalInfo = new Label(String.format("Total: %.2f TND", order.getTotalPrice()));
        totalInfo.getStyleClass().add("price-label");

        Label statusLabel = new Label(order.getStatus().toUpperCase());
        statusLabel.getStyleClass().add("badge");
        statusLabel.getStyleClass().add("badge-" + order.getStatus().toLowerCase());

        HBox buttonRow = new HBox(8);

        if (order.getStatus().equals("pending")) {
            Button validateBtn = new Button("✅ Validate");
            validateBtn.getStyleClass().add("success-button");
            validateBtn.setOnAction(e -> handleAdminValidateOrder(order));
            buttonRow.getChildren().add(validateBtn);
        }

        if (order.getStatus().equals("validated")) {
            Button deliverBtn = new Button("🚚 Mark Delivered");
            deliverBtn.getStyleClass().add("primary-button");
            deliverBtn.setOnAction(e -> handleMarkDelivered(order));
            buttonRow.getChildren().add(deliverBtn);
        }

        Button deleteBtn = new Button("🗑️");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> handleAdminDeleteOrder(order));
        buttonRow.getChildren().add(deleteBtn);

        card.getChildren().addAll(orderInfo, customerInfo, totalInfo, statusLabel, buttonRow);
        return card;
    }

    private VBox createUserOrderCard(Order order) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("product-card");
        card.setPrefWidth(360);

        Label orderInfo = new Label("Order #" + order.getId());
        orderInfo.getStyleClass().add("product-title");

        Label totalInfo = new Label(String.format("Total: %.2f TND", order.getTotalPrice()));
        totalInfo.getStyleClass().add("price-label");

        Label addressInfo = new Label("📍 " + order.getDeliveryAddress());
        addressInfo.getStyleClass().add("stock-label");

        Label paymentInfo = new Label("💳 " + order.getPaymentMethod());
        paymentInfo.getStyleClass().add("stock-label");

        // ✅ Date and time
        Label dateInfo = new Label("🕐 " + (order.getOrderDate() != null
                ? order.getOrderDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A"));
        dateInfo.getStyleClass().add("stock-label");

        Label statusLabel = new Label(order.getStatus().toUpperCase());
        statusLabel.getStyleClass().add("badge");
        statusLabel.getStyleClass().add("badge-" + order.getStatus().toLowerCase());

        card.getChildren().addAll(orderInfo, dateInfo, totalInfo, addressInfo, paymentInfo, statusLabel);

        if (order.getStatus().equals("pending")) {
            Button updateBtn = new Button("✏️ Update Details");
            updateBtn.getStyleClass().add("primary-button");
            updateBtn.setOnAction(e -> handleUpdateOrderDetails(order));

            Button cancelBtn = new Button("❌ Cancel Order");
            cancelBtn.getStyleClass().add("secondary-button");
            cancelBtn.setOnAction(e -> handleUserCancelOrder(order));

            HBox buttonRow = new HBox(8);
            buttonRow.getChildren().addAll(updateBtn, cancelBtn);
            card.getChildren().add(buttonRow);
        }

        return card;
    }


    private void handleUpdateOrderDetails(Order order) {

        String address;
        Double lat;
        Double lng;

        // Step 1 — Open map with existing stored location
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/fxml/marketplace/DeliveryMapDialog.fxml"
            ));
            Parent root = loader.load();

            DeliveryMapDialogController mapController = loader.getController();

            // IMPORTANT: pass existing location (from DB / Order object)
            if (order.getDeliveryLat() != null && order.getDeliveryLng() != null) {
                mapController.setInitialLocation(order.getDeliveryLat(), order.getDeliveryLng());
            }

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Update delivery location");
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (!mapController.isConfirmed()) {
                showAlert("Cancelled", "Update cancelled.", Alert.AlertType.WARNING);
                return;
            }

            address = mapController.getSelectedAddress();
            lat = mapController.getSelectedLatitude();
            lng = mapController.getSelectedLongitude();

        } catch (IOException e) {
            showAlert("Error", "Failed to open map: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        // Step 2 — Ask payment method (keep your existing dialog)
        ChoiceDialog<String> paymentDialog = new ChoiceDialog<>(
                order.getPaymentMethod(), "Cash on Delivery", "Credit Card", "Bank Transfer"
        );
        paymentDialog.setTitle("Update Payment Method");
        paymentDialog.setHeaderText("Update your payment method:");
        paymentDialog.setContentText("Payment:");
        String payment = paymentDialog.showAndWait().orElse("").trim();

        if (payment.isEmpty()) {
            showAlert("Cancelled", "Please select a payment method.", Alert.AlertType.WARNING);
            return;
        }

        // Step 3 — Save to DB (now includes lat/lng)
        try {
            orderService.updateOrderDetails(order.getId(), address, payment, lat, lng);
            loadUserOrders();
            showAlert("Success", "✅ Order updated successfully!", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Error", "Failed to update order: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }




    private void handleAdminValidateOrder(Order order) {
        try {
            orderService.updateOrderStatus(order.getId(), "validated");
            loadOrders();
            showAlert("Success", "Order validated!", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Error", "Failed to validate order: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleMarkDelivered(Order order) {
        try {
            orderService.updateOrderStatus(order.getId(), "delivered");
            loadOrders();
            showAlert("Success", "Order marked as delivered!", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Error", "Failed to update order: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleAdminDeleteOrder(Order order) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Order");
        confirm.setHeaderText("Are you sure?");

        // ✅ Different message based on status
        if (order.getStatus().equals("pending") || order.getStatus().equals("validated")) {
            confirm.setContentText("Delete order #" + order.getId() + "? Stock will be restored.");
        } else {
            confirm.setContentText("Delete order #" + order.getId() + " from history?");
        }

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // ✅ Only restore stock if order was NOT delivered/cancelled
                    if (order.getStatus().equals("pending") || order.getStatus().equals("validated")) {
                        orderService.cancelOrder(order.getId()); // restores stock + deletes
                    } else {
                        orderService.deleteOrder(order.getId()); // just deletes
                    }
                    loadOrders();
                    showAlert("Success", "Order deleted!", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Error", "Failed to delete order: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }


    private void handleUserCancelOrder(Order order) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Order");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Cancel order #" + order.getId() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    orderService.cancelOrder(order.getId());
                    loadUserOrders();
                    showAlert("Success", "Order cancelled and stock restored!", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Error", "Failed to cancel order: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
