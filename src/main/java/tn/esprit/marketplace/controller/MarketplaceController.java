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
        marketplaceTab.setVisible(false);
        manageProductsTab.setVisible(false);
        cartTab.setVisible(false);

        String inactiveStyle = "-fx-background-color: white; -fx-text-fill: #6b7280; -fx-padding: 16 32; -fx-background-radius: 0; -fx-font-size: 14px; -fx-font-weight: 600; -fx-border-width: 0;";
        String activeStyle = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-padding: 16 32; -fx-font-size: 14px; -fx-font-weight: 600; -fx-border-width: 0;";

        if (currentUser.equals("admin")) {
            btnManageProducts.setStyle(inactiveStyle + "-fx-background-radius: 8 0 0 0;");
            btnCart.setStyle(inactiveStyle + "-fx-background-radius: 0 8 0 0;");
        } else {
            btnMarketplace.setStyle(inactiveStyle + "-fx-background-radius: 8 0 0 0;");
            btnManageProducts.setStyle(inactiveStyle);
            btnCart.setStyle(inactiveStyle + "-fx-background-radius: 0 8 0 0;");
        }

        switch (tab) {
            case "marketplace":
                marketplaceTab.setVisible(true);
                btnMarketplace.setStyle(activeStyle + "-fx-background-radius: 8 0 0 0;");
                break;
            case "manage":
                manageProductsTab.setVisible(true);
                if (currentUser.equals("admin")) {
                    btnManageProducts.setStyle(activeStyle + "-fx-background-radius: 8 0 0 0;");
                } else {
                    btnManageProducts.setStyle(activeStyle);
                }
                break;
            case "cart":
            case "orders":
                cartTab.setVisible(true);
                btnCart.setStyle(activeStyle + "-fx-background-radius: 0 8 0 0;");
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
}