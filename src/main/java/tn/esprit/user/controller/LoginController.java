package tn.esprit.user.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.user.entity.Role;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;
import tn.esprit.utils.ValidationUtil;

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

// added import
import tn.esprit.navigation.MainLayoutController;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Button loginButton;
    @FXML private Hyperlink signupLink;
    @FXML private Hyperlink forgotPasswordLink;

    private UserCrud userCrud = new UserCrud();
    private Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    public void initialize() {
        // Load saved credentials if "Remember Me" was checked
        String savedEmail = prefs.get("savedEmail", "");
        boolean rememberMe = prefs.getBoolean("rememberMe", false);

        if (rememberMe && !savedEmail.isEmpty()) {
            emailField.setText(savedEmail);
            rememberMeCheckbox.setSelected(true);
        }

        // Allow pressing Enter on either field to trigger login
        emailField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill all fields");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid email format");
            emailField.requestFocus();
            return;
        }

        try {
            User user = userCrud.findByEmail(email);

            if (user != null && PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                // Check if email is verified
                if (!user.isEmailVerified()) {
                    showAlert(Alert.AlertType.WARNING, "Email Not Verified",
                        "Please verify your email address before logging in. Check your email for verification instructions.");
                    return;
                }

                SessionManager.getInstance().login(user);

                // Handle Remember Me for email
                System.out.println("=== SAVING PREFERENCES ===");
                System.out.println("Remember Me checkbox is: " + rememberMeCheckbox.isSelected());

                if (rememberMeCheckbox.isSelected()) {
                    prefs.put("savedEmail", email);
                    prefs.putBoolean("rememberMe", true);

                    // Save encrypted token for auto-login
                    TokenManager.saveToken(user.getId());

                    System.out.println("✅ SAVED email: " + email);
                    System.out.println("✅ SAVED auth token");
                } else {
                    prefs.remove("savedEmail");
                    prefs.putBoolean("rememberMe", false);

                    // Don't save token if Remember Me is unchecked
                    TokenManager.clearToken();

                    System.out.println("❌ CLEARED saved credentials");
                }

                // close login window
                Stage loginStage = (Stage) loginButton.getScene().getWindow();
                // load the main layout (wraps content with sidebar/topbar)
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
                    Scene scene = new Scene(loader.load());
                    // ensure stylesheet is applied (fallback if FXML stylesheet isn't picked up)
                    URL css = getClass().getResource("/styles/styles.css");
                    if (css != null) {
                        scene.getStylesheets().add(css.toExternalForm());
                    }

                    Stage stage = new Stage();
                    stage.setTitle("Digital Farm");
                    stage.setScene(scene);
                    stage.setMinWidth(1000); // ✅ minimum window size
                    stage.setMinHeight(700);
                    stage.setMaximized(true); // ✅ always full window
                    stage.show();
                    // close login after showing main window
                    loginStage.close();

                    // if admin, instruct main layout to open admin dashboard in the content area
                    MainLayoutController mainController = loader.getController();
                    if (user.getRole() == Role.ADMIN) {
                        mainController.openAdminDashboard();
                    }
                    // otherwise MainLayoutController.initialize already routes to CROPS by default

                } catch (IOException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load main layout: " + e.getMessage());
                    e.printStackTrace();
                }

            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid email or password");
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Login failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleSignupLink() {
        try {
            Stage stage = (Stage) signupLink.getScene().getWindow();
            stage.close();
            loadScene("/fxml/user/signup.fxml", "Sign Up");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load signup page: " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPasswordLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/forgot-password.fxml"));
            Scene scene = new Scene(loader.load());

            ForgotPasswordController controller = loader.getController();

            Stage forgotPasswordStage = new Stage();
            forgotPasswordStage.setTitle("Forgot Password - Agrinova");
            forgotPasswordStage.setScene(scene);
            forgotPasswordStage.setResizable(false);
            forgotPasswordStage.initModality(Modality.APPLICATION_MODAL);

            controller.setStage(forgotPasswordStage);
            forgotPasswordStage.showAndWait();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open forgot password dialog: " + e.getMessage());
        }
    }

    private void loadScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setTitle("Digital Farm - " + title);
        stage.setScene(scene);
        stage.setMinWidth(800); // Reasonable minimum size
        stage.setMinHeight(600);
        stage.centerOnScreen(); // Center the window instead of maximizing
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
