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
import tn.esprit.utils.EmailService;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.ValidationUtil;
import tn.esprit.utils.VerificationCodeManager;

import java.io.IOException;

public class SignupController {
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private Button togglePasswordBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisible;
    @FXML private Button toggleConfirmPasswordBtn;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label confirmPasswordErrorLabel;
    @FXML private Button signupButton;
    @FXML private Hyperlink loginLink;

    private boolean passwordShown = false;
    private boolean confirmPasswordShown = false;

    private UserCrud userCrud = new UserCrud();
    private EmailService emailService = new EmailService();
    private VerificationCodeManager verificationManager = VerificationCodeManager.getInstance();

    @FXML
    public void initialize() {
        setupRealtimeValidation();

        // Allow pressing Enter to move between fields and trigger signup
        fullNameField.setOnAction(event -> emailField.requestFocus());
        emailField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(event -> handleSignup());
    }

    @FXML
    private void togglePassword() {
        passwordShown = !passwordShown;
        if (passwordShown) {
            passwordVisible.setText(passwordField.getText());
            passwordVisible.setVisible(true);
            passwordVisible.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
        } else {
            passwordField.setText(passwordVisible.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            togglePasswordBtn.setText("👁");
        }
    }

    @FXML
    private void toggleConfirmPassword() {
        confirmPasswordShown = !confirmPasswordShown;
        if (confirmPasswordShown) {
            confirmPasswordVisible.setText(confirmPasswordField.getText());
            confirmPasswordVisible.setVisible(true);
            confirmPasswordVisible.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            toggleConfirmPasswordBtn.setText("🙈");
        } else {
            confirmPasswordField.setText(confirmPasswordVisible.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordVisible.setVisible(false);
            confirmPasswordVisible.setManaged(false);
            toggleConfirmPasswordBtn.setText("👁");
        }
    }

    private void setupRealtimeValidation() {
        // Full name validation
        fullNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                if (ValidationUtil.isValidFullName(newVal)) {
                    fullNameField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                } else {
                    fullNameField.setStyle("-fx-border-color: #FFA726; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                }
            } else {
                fullNameField.setStyle("");
            }
        });

        // Email validation
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                if (ValidationUtil.isValidEmail(newVal)) {
                    emailField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                } else {
                    emailField.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                }
            } else {
                emailField.setStyle("");
            }
        });

        // Password validation with strength indicator + inline error
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
            if (!newVal.isEmpty()) {
                String msg = ValidationUtil.getPasswordValidationMessage(newVal);
                if (msg.isEmpty()) {
                    passwordField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(passwordErrorLabel, null);
                } else {
                    passwordField.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(passwordErrorLabel, msg);
                }
            } else {
                passwordField.setStyle("");
                setFieldError(passwordErrorLabel, null);
            }
            // Re-validate confirm field too whenever password changes
            String confirm = confirmPasswordShown ? confirmPasswordVisible.getText() : confirmPasswordField.getText();
            if (!confirm.isEmpty()) {
                if (confirm.equals(newVal)) {
                    confirmPasswordField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(confirmPasswordErrorLabel, null);
                } else {
                    confirmPasswordField.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(confirmPasswordErrorLabel, "Passwords do not match");
                }
            }
        });

        // Also mirror listener on visible text field
        passwordVisible.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
            if (!newVal.isEmpty()) {
                String msg = ValidationUtil.getPasswordValidationMessage(newVal);
                if (msg.isEmpty()) {
                    passwordVisible.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(passwordErrorLabel, null);
                } else {
                    passwordVisible.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(passwordErrorLabel, msg);
                }
            } else {
                passwordVisible.setStyle("");
                setFieldError(passwordErrorLabel, null);
            }
        });

        // Confirm password validation with inline error
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            String pw = passwordShown ? passwordVisible.getText() : passwordField.getText();
            if (!newVal.isEmpty()) {
                if (newVal.equals(pw)) {
                    confirmPasswordField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(confirmPasswordErrorLabel, null);
                } else {
                    confirmPasswordField.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(confirmPasswordErrorLabel, "Passwords do not match");
                }
            } else {
                confirmPasswordField.setStyle("");
                setFieldError(confirmPasswordErrorLabel, null);
            }
        });

        confirmPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> {
            String pw = passwordShown ? passwordVisible.getText() : passwordField.getText();
            if (!newVal.isEmpty()) {
                if (newVal.equals(pw)) {
                    confirmPasswordVisible.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(confirmPasswordErrorLabel, null);
                } else {
                    confirmPasswordVisible.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
                    setFieldError(confirmPasswordErrorLabel, "Passwords do not match");
                }
            } else {
                confirmPasswordVisible.setStyle("");
                setFieldError(confirmPasswordErrorLabel, null);
            }
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthBar.setProgress(0);
            passwordStrengthLabel.setText("");
            return;
        }

        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*\\d.*")) strength++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strength++;

        double progress = strength / 6.0;
        String strengthText;
        String color;

        if (strength <= 2) {
            strengthText = "Weak";
            color = "#EF5350";
        } else if (strength <= 4) {
            strengthText = "Medium";
            color = "#FFA726";
        } else {
            strengthText = "Strong";
            color = "#2E7D32";
        }

        passwordStrengthBar.setProgress(progress);
        passwordStrengthBar.setStyle("-fx-accent: " + color + ";");
        passwordStrengthLabel.setText(strengthText);
        passwordStrengthLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    @FXML
    private void handleSignup() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordShown ? passwordVisible.getText() : passwordField.getText();
        String confirmPassword = confirmPasswordShown ? confirmPasswordVisible.getText() : confirmPasswordField.getText();

        // Validate full name
        String nameError = ValidationUtil.getFullNameValidationMessage(fullName);
        if (!nameError.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", nameError);
            fullNameField.requestFocus();
            return;
        }

        // Validate email
        String emailError = ValidationUtil.getEmailValidationMessage(email);
        if (!emailError.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", emailError);
            emailField.requestFocus();
            return;
        }

        // Validate password
        String passwordError = ValidationUtil.getPasswordValidationMessage(password);
        if (!passwordError.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", passwordError);
            passwordField.requestFocus();
            return;
        }

        // Check password confirmation
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Passwords do not match");
            confirmPasswordField.requestFocus();
            return;
        }

        try {
            // Check if email already exists
            if (userCrud.findByEmail(email) != null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Email already registered");
                emailField.requestFocus();
                return;
            }

            // Create user object (don't save to database yet)
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPasswordHash(PasswordUtil.hashPassword(password));
            user.setRole(Role.USER);
            user.setEmailVerified(false);

            // Generate and send verification code
            signupButton.setDisable(true);
            signupButton.setText("Processing...");

            String verificationCode = emailService.generateVerificationCode();
            boolean emailSent = emailService.sendVerificationEmail(email, fullName, verificationCode);

            if (emailSent) {
                // Store verification code
                verificationManager.storeVerificationCode(email, verificationCode);

                // Show verification dialog immediately (email sends in background)
                showAlert(Alert.AlertType.INFORMATION, "Email Sent",
                    "Verification code has been sent to " + email + "\nCheck your email and enter the code in the next dialog.");

                openEmailVerificationDialog(user);

            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to send verification email. Please try again.");
            }

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Registration failed: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            signupButton.setDisable(false);
            signupButton.setText("Create Account");
        }
    }

    @FXML
    private void handleLoginLink() {
        try {
            Stage stage = (Stage) loginLink.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            java.net.URL css = getClass().getResource("/styles/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("AgriNova - Login");
            stage.setScene(scene);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load login page: " + e.getMessage());
        }
    }

    private void loadScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setTitle("AgriNova - " + title);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.centerOnScreen();
        stage.show();
    }

    private void openEmailVerificationDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/email-verification.fxml"));
            Scene scene = new Scene(loader.load());

            EmailVerificationController controller = loader.getController();
            controller.setPendingUser(user);
            controller.setPasswordResetMode(false);

            Stage currentStage = (Stage) signupButton.getScene().getWindow();

            Stage verificationStage = new Stage();
            verificationStage.setTitle("Email Verification - AgriNova");
            verificationStage.setScene(scene);
            verificationStage.setResizable(false);
            verificationStage.initModality(Modality.APPLICATION_MODAL);
            verificationStage.initOwner(currentStage);

            controller.setStage(verificationStage);
            controller.initializeDialog();


            verificationStage.showAndWait();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open verification dialog.");
            e.printStackTrace();
        }
    }

    private void setFieldError(Label label, String message) {
        if (label == null) return;
        if (message == null || message.isEmpty()) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        } else {
            label.setText("✖  " + message);
            label.setVisible(true);
            label.setManaged(true);
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

