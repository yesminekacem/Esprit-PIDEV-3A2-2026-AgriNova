package tn.esprit.user.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import tn.esprit.navigation.Router;          // ✅ ADD THIS
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.IOException;

public class AdminDashboardController {

    @FXML private Label adminNameLabel;
    @FXML private StackPane contentArea;
    @FXML private Button logoutButton;
    @FXML private ImageView topbarAvatarView;

    @FXML
    public void initialize() {
        // Hide avatar ImageView by default (emoji label shows instead)
        if (topbarAvatarView != null) topbarAvatarView.setVisible(false);

        tn.esprit.user.entity.User u = SessionManager.getInstance().getCurrentUser();
        if (u != null) {
            adminNameLabel.setText(u.getFullName());
            // Load profile image into topbar avatar
            if (u.getProfileImage() != null && !u.getProfileImage().isEmpty()) {
                java.io.File f = new java.io.File(u.getProfileImage());
                if (f.exists() && topbarAvatarView != null) {
                    topbarAvatarView.setImage(new Image(f.toURI().toString()));
                    topbarAvatarView.setClip(new Circle(20, 20, 20));
                    topbarAvatarView.setVisible(true);
                }
            }
        }

        // ✅ IMPORTANT: allow Router.go(...) to inject pages inside the dashboard center
        Router.init(contentArea);

        // Open users list as default view
        loadIntoContent("/fxml/user/UsersList.fxml");
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
