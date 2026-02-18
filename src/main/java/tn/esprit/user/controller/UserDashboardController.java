package tn.esprit.user.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.navigation.Router;
import tn.esprit.user.entity.User;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.IOException;

public class UserDashboardController {

    @FXML private StackPane contentArea;

    @FXML private Label welcomeLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label roleValueLabel;

    @FXML private Button logoutButton;

    @FXML
    public void initialize() {
        Router.init(contentArea);   // ✅ IMPORTANT

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            nameLabel.setText(currentUser.getFullName());
            emailValueLabel.setText(currentUser.getEmail());
            roleValueLabel.setText(String.valueOf(currentUser.getRole()));
        }
    }

    private void setContent(String fxmlPath) {
        System.out.println("Loading view: " + fxmlPath);
        System.out.println("Resolved URL: " + getClass().getResource(fxmlPath));

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Cannot load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    @FXML
    private void openDashboard() {
        // optional: if you create a separate dashboard page later
    }
//
    @FXML
    private void openForum() {setContent("/fxml/forum/ForumView.fxml");}
    @FXML
    private void openMarketplace() {
        setContent("/fxml/marketplace/MarketplaceView.fxml");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        TokenManager.clearToken();

        try {
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setTitle("Digital Farm - Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}