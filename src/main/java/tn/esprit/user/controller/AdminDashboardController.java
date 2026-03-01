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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(scene);
            loginStage.show();

            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error");
            a.setHeaderText(null);
            a.setContentText("Failed to open login.fxml: " + e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    private void openAdminDashboard(ActionEvent e) {}

    @FXML
    private void openAdminUsers(ActionEvent e) {}

    @FXML
    private void openForum(ActionEvent e) {
        loadIntoContent("/fxml/forum/ForumView.fxml");
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
}