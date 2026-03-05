package tn.esprit.user.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.navigation.Router;          // ✅ ADD THIS
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private Label adminNameLabel;
    @FXML private StackPane contentArea;
    @FXML private Button logoutButton;

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            adminNameLabel.setText(SessionManager.getInstance().getCurrentUserName());
        }

        // ✅ IMPORTANT: allow Router.go(...) to inject pages inside the dashboard center
        Router.init(contentArea);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout();
        TokenManager.clearToken();
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        tn.esprit.MainFX.loadLoginOnStage(stage);
    }

    @FXML
    private void openAdminDashboard(ActionEvent e) {}

    @FXML
    private void openAdminUsers(ActionEvent e) {loadIntoContent("/fxml/user/UsersList.fxml");}

    @FXML
    private void openSettings(ActionEvent e) {loadIntoContent("/fxml/user/user-dashboard.fxml");}

    @FXML
    private void openForum(ActionEvent e) {
        loadIntoContent("/fxml/forum/ForumView.fxml");
    }
    @FXML
    private void openMarketplace(ActionEvent e) {
        loadIntoContent("/fxml/marketplace/MarketplaceView.fxml");
    }

    private void loadIntoContent(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error");
            a.setHeaderText(null);
            a.setContentText("Failed to load view: " + fxmlPath + "\n" + ex.getMessage());
            a.showAndWait();
        }
    }
    //Crop
    @FXML
    private void handleManageCrops() {
        Router.go("/fxml/crop/CropView.fxml");
    }

    @FXML
    private void openInventory(ActionEvent e) {
        loadIntoContent("/fxml/inventory/InventoryView.fxml");
    }

    @FXML
    private void openCalendar(ActionEvent e) {
        loadIntoContent("/fxml/inventory/CalendarView.fxml");
    }

    @FXML
    private void openDashboard(ActionEvent e) {
        loadIntoContent("/fxml/inventory/DashboardView.fxml");
    }
}
