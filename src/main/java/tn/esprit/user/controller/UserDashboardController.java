package tn.esprit.user.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.IOException;

public class UserDashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label roleValueLabel;
    @FXML private Button logoutButton;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            nameLabel.setText(currentUser.getFullName());
            emailValueLabel.setText(currentUser.getEmail());
            roleValueLabel.setText(currentUser.getRole().toString());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        TokenManager.clearToken(); // Clear token on logout

        try {
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.close();
            loadScene("/fxml/user/login.fxml", "Login");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to logout: " + e.getMessage());
        }
    }


    private void loadScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setTitle("Digital Farm - " + title);
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
