package tn.esprit.user.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.user.entity.Role;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class UserDashboardController {

    // ── Layout ────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ── Avatar / header ───────────────────────────────────────────────
    @FXML private ImageView profileImageView;
    @FXML private Label welcomeLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label roleValueLabel;
    @FXML private Label roleChipLabel;
    @FXML private Button changePicButton;

    // ── Personal info card ────────────────────────────────────────────
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private Button saveNameEmailButton;

    // ── Password card ─────────────────────────────────────────────────
    @FXML private VBox currentPasswordBox;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordFeedbackLabel;
    @FXML private Button savePasswordButton;

    // ── Face ID card ──────────────────────────────────────────────────
    @FXML private Label faceIdStatusLabel;
    @FXML private Button faceIdButton;

    // ── Logout (may be in sidebar, handle gracefully) ─────────────────
    @FXML private Button logoutButton;

    private final UserCrud userCrud = new UserCrud();

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user session found.");
            return;
        }

        populateUI(current);

        // Admins don't need to enter current password
        boolean isAdmin = current.getRole() == Role.ADMIN;
        if (currentPasswordBox != null) {
            currentPasswordBox.setVisible(!isAdmin);
            currentPasswordBox.setManaged(!isAdmin);
        }

        // If loaded standalone (no sidebar), wrap in MainLayout
        Platform.runLater(() -> {
            Scene scene = contentArea.getScene();
            if (scene == null) return;
            Node sidebar = scene.lookup(".sidebar");
            if (sidebar == null) {
                try {
                    Stage stage = (Stage) scene.getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
                    Parent root = loader.load();
                    Scene mainScene = new Scene(root);
                    java.net.URL css = getClass().getResource("/styles/styles.css");
                    if (css != null) mainScene.getStylesheets().add(css.toExternalForm());
                    stage.setScene(mainScene);
                    stage.setMinWidth(1200);
                    stage.setMinHeight(700);
                    stage.setMaximized(true);
                    stage.show();
                    Object ctrl = loader.getController();
                    if (ctrl instanceof tn.esprit.navigation.MainLayoutController)
                        ((tn.esprit.navigation.MainLayoutController) ctrl).openSettings();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void populateUI(User u) {
        fullNameField.setText(u.getFullName());
        emailField.setText(u.getEmail());
        welcomeLabel.setText("Welcome back, " + u.getFullName() + "!");
        nameLabel.setText(u.getFullName());
        emailValueLabel.setText(u.getEmail());
        String roleText = u.getRole().name();
        roleValueLabel.setText(roleText);
        if (roleChipLabel != null) roleChipLabel.setText(roleText);
        refreshFaceIdStatus(u);

        String imgPath = u.getProfileImage();
        if (imgPath != null && !imgPath.isEmpty()) {
            File f = new File(imgPath);
            if (f.exists()) profileImageView.setImage(new Image(f.toURI().toString()));
        }
    }

    // ── Personal info ─────────────────────────────────────────────────
    @FXML
    private void handleSaveNameEmail() {
        try {
            User current = SessionManager.getInstance().getCurrentUser();
            if (current == null) return;

            String name  = fullNameField.getText().trim();
            String email = emailField.getText().trim();

            if (name.isEmpty() || email.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Name and email are required.");
                return;
            }

            current.setFullName(name);
            current.setEmail(email);
            userCrud.update(current);

            User refreshed = userCrud.findById(current.getId());
            SessionManager.getInstance().login(refreshed);
            populateUI(refreshed);
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Personal information updated ✅");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }

    // ── Change password ───────────────────────────────────────────────
    @FXML
    private void handleSavePassword() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) return;

        boolean isAdmin = current.getRole() == Role.ADMIN;

        // Non-admins must verify current password
        if (!isAdmin) {
            String entered = currentPasswordField.getText();
            if (entered.isEmpty()) {
                setPasswordFeedback("⚠  Please enter your current password.", "#ef4444");
                return;
            }
            if (!PasswordUtil.checkPassword(entered, current.getPasswordHash())) {
                setPasswordFeedback("✖  Current password is incorrect.", "#ef4444");
                currentPasswordField.clear();
                return;
            }
        }

        String newPw  = newPasswordField.getText();
        String confPw = confirmPasswordField.getText();

        if (newPw.isEmpty()) {
            setPasswordFeedback("⚠  New password cannot be empty.", "#ef4444");
            return;
        }

        // Full password validation (same rules as signup)
        String pwError = tn.esprit.utils.ValidationUtil.getPasswordValidationMessage(newPw);
        if (!pwError.isEmpty()) {
            setPasswordFeedback("✖  " + pwError, "#ef4444");
            return;
        }

        if (!newPw.equals(confPw)) {
            setPasswordFeedback("✖  Passwords do not match.", "#ef4444");
            return;
        }

        try {
            current.setPasswordHash(PasswordUtil.hashPassword(newPw));
            userCrud.update(current);
            SessionManager.getInstance().login(userCrud.findById(current.getId()));

            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            setPasswordFeedback("✔  Password updated successfully!", "#16a34a");
        } catch (SQLException e) {
            setPasswordFeedback("✖  Database error: " + e.getMessage(), "#ef4444");
        }
    }

    private void setPasswordFeedback(String msg, String color) {
        if (passwordFeedbackLabel == null) return;
        passwordFeedbackLabel.setText(msg);
        passwordFeedbackLabel.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
    }

    // ── Profile picture ───────────────────────────────────────────────
    @FXML
    private void handleChangePicture() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Picture");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        File file = chooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (file != null) {
            profileImageView.setImage(new Image(file.toURI().toString()));
            current.setProfileImage(file.getAbsolutePath());
            try {
                userCrud.update(current);
                SessionManager.getInstance().login(userCrud.findById(current.getId()));
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not save picture: " + e.getMessage());
            }
        }
    }

    // ── Face ID ───────────────────────────────────────────────────────
    private void refreshFaceIdStatus(User user) {
        if (faceIdStatusLabel == null) return;
        boolean has = user != null && user.getFaceData() != null && !user.getFaceData().isEmpty();
        faceIdStatusLabel.setText(has
                ? "✔  Registered — you can sign in with your face"
                : "Not set up — register your face to enable quick login");
        faceIdStatusLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + (has ? "#16a34a" : "#64748b") + ";");
        if (faceIdButton != null) faceIdButton.setText(has ? "⚙  Update Face ID" : "⚙  Set Up Face ID");
    }

    @FXML
    private void handleSetupFaceId() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/face-setup.fxml"));
            Scene scene = new Scene(loader.load());
            FaceSetupController ctrl = loader.getController();
            Stage dialog = new Stage();
            dialog.setTitle("Face ID Setup");
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setOnCloseRequest(e -> ctrl.onClose());
            dialog.showAndWait();
            refreshFaceIdStatus(SessionManager.getInstance().getCurrentUser());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot open Face ID setup: " + e.getMessage());
        }
    }

    // ── Navigation (called by MainLayout sidebar) ─────────────────────
    private void setContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Cannot load: " + fxmlPath);
        }
    }

    @FXML private void openDashboard()   {}
    @FXML private void openForum()       { setContent("/fxml/forum/ForumView.fxml"); }
    @FXML private void openMarketplace() { setContent("/fxml/marketplace/MarketplaceView.fxml"); }
    @FXML private void openCrops()       { setContent("/fxml/crop/CropsPage.fxml"); }
    @FXML private void openSettings()    { setContent("/fxml/user/user-dashboard.fxml"); }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        TokenManager.clearToken();
        Stage stage = (Stage) (logoutButton != null
                ? logoutButton.getScene().getWindow()
                : contentArea.getScene().getWindow());
        tn.esprit.MainFX.loadLoginOnStage(stage);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
