package tn.esprit.marketplace.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;

public class MarketplaceController {

    private String currentUser = "user2";

    @FXML private Label lblUserInfo;
    @FXML private Button btnMarketplace;
    @FXML private Button btnManageProducts;
    @FXML private Button btnCart;
    @FXML private VBox marketplaceTab;
    @FXML private VBox manageProductsTab;
    @FXML private VBox cartTab;

    @FXML
    public void initialize() {
        initializeTabButtons();
        updateUserInfo();
        updateTabsForUser();
    }

    @FXML
    private void showMarketplace() {
        switchTab("marketplace");
        loadView("/fxml/marketplace/ProductListingView.fxml", marketplaceTab);
    }

    @FXML
    private void showManageProducts() {
        switchTab("manage");
        loadView("/fxml/marketplace/ManageProductsView.fxml", manageProductsTab);
    }

    @FXML
    private void showCart() {
        if (currentUser.equals("admin")) {
            switchTab("orders");
            loadView("/fxml/marketplace/OrdersView.fxml", cartTab);
        } else {
            Dialog<String> choiceDialog = new Dialog<>();
            choiceDialog.setTitle("View");
            choiceDialog.setHeaderText("What would you like to view?");

            ButtonType cartButton = new ButtonType("🛒 Cart", ButtonBar.ButtonData.OK_DONE);
            ButtonType ordersButton = new ButtonType("📦 My Orders", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            choiceDialog.getDialogPane().getButtonTypes().addAll(cartButton, ordersButton, cancelButton);

            choiceDialog.setResultConverter(button -> {
                if (button == cartButton) return "cart";
                if (button == ordersButton) return "orders";
                return null;
            });

            choiceDialog.showAndWait().ifPresent(choice -> {
                switchTab("cart");
                if (choice.equals("cart")) {
                    loadView("/fxml/marketplace/CartView.fxml", cartTab);
                } else {
                    loadView("/fxml/marketplace/OrdersView.fxml", cartTab);
                }
            });
        }
    }

    private void loadView(String fxmlPath, VBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            VBox view = loader.load();

            container.getChildren().clear();
            container.getChildren().add(view);
            VBox.setVgrow(view, Priority.ALWAYS);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load view: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void switchTab(String tab) {
        // Hide all tabs
        marketplaceTab.setVisible(false);
        manageProductsTab.setVisible(false);
        cartTab.setVisible(false);

        // Reset ALL buttons to inactive state
        btnMarketplace.getStyleClass().removeAll("tab-active", "tab-left");
        btnMarketplace.getStyleClass().add("tab-inactive");

        btnManageProducts.getStyleClass().removeAll("tab-active");
        btnManageProducts.getStyleClass().add("tab-inactive");

        btnCart.getStyleClass().removeAll("tab-active", "tab-right");
        btnCart.getStyleClass().add("tab-inactive");

        // Show correct tab and activate button
        switch (tab) {
            case "marketplace":
                marketplaceTab.setVisible(true);
                btnMarketplace.getStyleClass().remove("tab-inactive");
                btnMarketplace.getStyleClass().addAll("tab-active", "tab-left");
                break;
            case "manage":
                manageProductsTab.setVisible(true);
                btnManageProducts.getStyleClass().remove("tab-inactive");
                btnManageProducts.getStyleClass().add("tab-active");
                break;
            case "cart":
            case "orders":
                cartTab.setVisible(true);
                btnCart.getStyleClass().remove("tab-inactive");
                btnCart.getStyleClass().addAll("tab-active", "tab-right");
                break;
        }
    }


    private void updateUserInfo() {
        String displayName = currentUser.equals("admin") ? "Admin" :
                currentUser.equals("user1") ? "User1" : "User2";
        lblUserInfo.setText("Logged in as: " + displayName);
    }

    private void updateTabsForUser() {
        if (currentUser.equals("admin")) {
            btnMarketplace.setVisible(false);
            btnMarketplace.setManaged(false);
            btnCart.setText("Orders");
            showManageProducts();
        } else {
            btnMarketplace.setVisible(true);
            btnMarketplace.setManaged(true);
            btnCart.setText("Cart");
            showMarketplace();
        }
    }


    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void initializeTabButtons() {
        btnMarketplace.getStyleClass().addAll("tab-inactive", "tab-left");
        btnManageProducts.getStyleClass().add("tab-inactive");
        btnCart.getStyleClass().addAll("tab-inactive", "tab-right");
    }
}