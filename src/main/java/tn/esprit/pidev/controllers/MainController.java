package tn.esprit.pidev.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main layout controller with sidebar navigation.
 * Loads existing Inventory and Rental views into the center content pane.
 */
public class MainController implements Initializable {

    @FXML private Button dashboardBtn;
    @FXML private Button usersBtn;
    @FXML private Button cropsBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button calendarBtn;
    @FXML private Button marketplaceBtn;
    @FXML private Button forumBtn;
    @FXML private Button settingsBtn;

    @FXML private StackPane contentPane;

    private Button currentActiveButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // When scene is ready, expose this controller via root properties
        contentPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getRoot() != null) {
                newScene.getRoot().getProperties().put("mainController", this);
            }
        });

        currentActiveButton = inventoryBtn;
        showInventory();
    }

    @FXML
    private void showDashboard() {
        setActiveButton(dashboardBtn);
        loadView("/fxml/DashboardView.fxml", "/css/dashboard-styles.css");
    }

    @FXML
    private void showInventory() {
        setActiveButton(inventoryBtn);
        loadView("/fxml/InventoryView.fxml", "/css/inventory-styles.css");
    }

    /**
     * Show rentals module. Called from Inventory (no separate nav item).
     */
    public void showRentals() {
        // Keep Inventory highlighted as active section
        loadView("/fxml/RentalView.fxml", "/css/rental-styles.css");
    }

    @FXML
    private void showUsers() {
        setActiveButton(usersBtn);
        showComingSoon("Users Management");
    }

    @FXML
    private void showCrops() {
        setActiveButton(cropsBtn);
        showComingSoon("Crops");
    }

    @FXML
    private void showCalendar() {
        setActiveButton(calendarBtn);
        loadView("/fxml/CalendarView.fxml", "/css/calendar-styles.css");
    }

    @FXML
    private void showMarketplace() {
        setActiveButton(marketplaceBtn);
        showComingSoon("Marketplace");
    }

    @FXML
    private void showForum() {
        setActiveButton(forumBtn);
        showComingSoon("Forum");
    }

    @FXML
    private void showSettings() {
        setActiveButton(settingsBtn);
        showComingSoon("Settings");
    }

    private void loadView(String fxmlPath, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Apply view-specific stylesheet if available
            if (cssPath != null) {
                URL cssUrl = getClass().getResource(cssPath);
                if (cssUrl != null) {
                    view.getStylesheets().add(cssUrl.toExternalForm());
                }
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);

            FadeTransition fade = new FadeTransition(Duration.millis(200), view);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        } catch (Exception e) {
            e.printStackTrace();
            showComingSoon("View not found: " + fxmlPath);
        }
    }

    private void setActiveButton(Button btn) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("nav-active");
        }
        if (!btn.getStyleClass().contains("nav-active")) {
            btn.getStyleClass().add("nav-active");
        }
        currentActiveButton = btn;
    }

    private void showComingSoon(String feature) {
        StackPane placeholderRoot = new StackPane();
        placeholderRoot.setStyle("-fx-alignment: center; -fx-background-color: #FAF8F5;");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10);
        box.setStyle("-fx-alignment: center;");

        javafx.scene.control.Label icon = new javafx.scene.control.Label("🚧");
        icon.setStyle("-fx-font-size: 36px;");
        javafx.scene.control.Label title = new javafx.scene.control.Label(feature);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        javafx.scene.control.Label subtitle = new javafx.scene.control.Label("This section is coming soon.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #757575;");

        box.getChildren().addAll(icon, title, subtitle);
        placeholderRoot.getChildren().add(box);

        contentPane.getChildren().clear();
        contentPane.getChildren().add(placeholderRoot);
    }
}