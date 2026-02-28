package tn.esprit.user.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import tn.esprit.navigation.MainLayoutController;

public class UserDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label welcomeLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label roleValueLabel;
    @FXML private Button logoutButton;

    // ✅ Profile fields
    @FXML private ImageView profileImageView;
    @FXML private javafx.scene.control.TextField fullNameField;
    @FXML private javafx.scene.control.TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button changePicButton;
    @FXML private Button saveProfileButton;

    private final UserCrud userCrud = new UserCrud();

    @FXML
    public void initialize() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user session");
            return;
        }

        // Load profile data
        fullNameField.setText(current.getFullName());
        emailField.setText(current.getEmail());

        // Load profile image (handles null gracefully)
        String imgPath = current.getProfileImage();
        if (imgPath != null && !imgPath.isEmpty()) {
            File file = new File(imgPath);
            if (file.exists()) {
                profileImageView.setImage(new Image(file.toURI().toString()));
            }
        }

        // Update labels
        welcomeLabel.setText("Welcome, " + current.getFullName() + "!");
        nameLabel.setText(current.getFullName());
        emailValueLabel.setText(current.getEmail());
        roleValueLabel.setText(current.getRole().name());

        // If this FXML was loaded as a top-level scene (no sidebar present),
        // load the MainLayout around it and instruct the layout to display this dashboard.
        Platform.runLater(() -> {
            Scene scene = contentArea.getScene();
            if (scene == null) return;

            Node sidebar = scene.lookup(".sidebar");
            if (sidebar == null) {
                try {
                    Stage stage = (Stage) scene.getWindow();
                    // load MainLayout normally (don't override controller declared in FXML)
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
                    Parent root = loader.load();

                    Scene mainScene = new Scene(root);
                    // ensure stylesheet is applied (fallback)
                    java.net.URL css = getClass().getResource("/styles/styles.css");
                    if (css != null) mainScene.getStylesheets().add(css.toExternalForm());

                    stage.setScene(mainScene);
                    stage.setMinWidth(1200); // ✅ minimum window size
                    stage.setMinHeight(700);
                    stage.setMaximized(true);
                    stage.show();

                    // get the controller created by FXMLLoader and ask it to show this dashboard
                    Object ctrl = loader.getController();
                    if (ctrl instanceof tn.esprit.navigation.MainLayoutController) {
                        ((tn.esprit.navigation.MainLayoutController) ctrl).openSettings();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to wrap dashboard with main layout: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleChangePicture() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Picture");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = chooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (file != null) {
            profileImageView.setImage(new Image(file.toURI().toString()));
            current.setProfileImage(file.getAbsolutePath());
            System.out.println("✅ Image selected: " + file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSaveProfile() {
        try {
            User current = SessionManager.getInstance().getCurrentUser();
            if (current == null) return;

            String newName = fullNameField.getText().trim();
            String newEmail = emailField.getText().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Name and email required");
                return;
            }

            current.setFullName(newName);
            current.setEmail(newEmail);

            String newPassword = passwordField.getText();
            if (!newPassword.isEmpty()) {
                if (newPassword.length() < 6) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Password must be 6+ characters");
                    return;
                }
                current.setPasswordHash(PasswordUtil.hashPassword(newPassword));
            }

            boolean success = userCrud.update(current);
            if (!success) {
                showAlert(Alert.AlertType.ERROR, "Error", "Update failed - no changes made");
                return;
            }

            User refreshed = userCrud.findById(current.getId());
            SessionManager.getInstance().login(refreshed);

            welcomeLabel.setText("Welcome, " + newName + "!");
            nameLabel.setText(newName);
            emailValueLabel.setText(newEmail);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated! ✅");
            passwordField.clear();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    // ✅ Your existing navigation (unchanged)
    private void setContent(String fxmlPath) {
        System.out.println("Loading view: " + fxmlPath);
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

    @FXML private void openDashboard() {}
    @FXML private void openForum() {setContent("/fxml/forum/ForumView.fxml");}
    @FXML private void openMarketplace() {setContent("/fxml/marketplace/MarketplaceView.fxml");}
    @FXML private void openCrops() {setContent("/fxml/crop/CropsPage.fxml");}
    @FXML private void openSettings() { setContent("/fxml/user/user-dashboard.fxml"); }

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
