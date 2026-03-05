package tn.esprit.user.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.ValidationUtil;

public class PasswordResetController {
    @FXML private Label emailLabel;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ProgressBar passwordStrengthBar;
    @FXML private Label passwordStrengthLabel;
    @FXML private Label errorLabel;
    @FXML private Button resetButton;
    @FXML private Button cancelButton;

    private String userEmail;
    private UserCrud userCrud;
    private Stage stage;

    public void initialize() {
        userCrud = new UserCrud();
        setupPasswordValidation();
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
        emailLabel.setText(email);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setupPasswordValidation() {
        // Password strength validation
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
            clearError();

            if (!newVal.isEmpty()) {
                if (ValidationUtil.isValidPassword(newVal)) {
                    newPasswordField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 5;");
                } else {
                    newPasswordField.setStyle("-fx-border-color: #FFA726; -fx-border-width: 2; -fx-border-radius: 5;");
                }
            } else {
                newPasswordField.setStyle("");
            }
        });

        // Confirm password validation
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearError();

            if (!newVal.isEmpty()) {
                if (newVal.equals(newPasswordField.getText())) {
                    confirmPasswordField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 5;");
                } else {
                    confirmPasswordField.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 5;");
                }
            } else {
                confirmPasswordField.setStyle("");
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
        passwordStrengthLabel.setText(strengthText);
        passwordStrengthLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    @FXML
    private void handleResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate new password
        String passwordError = ValidationUtil.getPasswordValidationMessage(newPassword);
        if (!passwordError.isEmpty()) {
            showError(passwordError);
            newPasswordField.requestFocus();
            return;
        }

        // Check password confirmation
        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            confirmPasswordField.requestFocus();
            return;
        }

        try {
            resetButton.setDisable(true);
            resetButton.setText("Resetting...");

            // Find user by email
            User user = userCrud.findByEmail(userEmail);
            if (user == null) {
                showError("User not found. Please try again.");
                return;
            }

            // Update password
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            user.setPasswordHash(hashedPassword);

            boolean updated = userCrud.update(user);

            if (updated) {
                showSuccessAlert("Password reset successfully! You can now login with your new password.");
                Platform.runLater(() -> stage.close());
            } else {
                showError("Failed to reset password. Please try again.");
            }

        } catch (Exception e) {
            showError("An error occurred while resetting password. Please try again.");
            e.printStackTrace();
        } finally {
            resetButton.setDisable(false);
            resetButton.setText("Reset Password");
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }


    private void clearError() {
        errorLabel.setText("");
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
