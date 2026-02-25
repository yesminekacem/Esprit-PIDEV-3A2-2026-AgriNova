package tn.esprit.user.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.EmailService;
import tn.esprit.utils.ValidationUtil;
import tn.esprit.utils.VerificationCodeManager;

import java.io.IOException;

public class ForgotPasswordController {
    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Button sendCodeButton;
    @FXML private Button cancelButton;

    private UserCrud userCrud;
    private EmailService emailService;
    private VerificationCodeManager verificationManager;
    private Stage stage;

    public void initialize() {
        userCrud = new UserCrud();
        emailService = new EmailService();
        verificationManager = VerificationCodeManager.getInstance();

        // Setup email field validation
        emailField.textProperty().addListener((obs, oldText, newText) -> {
            clearError();
            if (!newText.trim().isEmpty()) {
                if (ValidationUtil.isValidEmail(newText)) {
                    emailField.setStyle("-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 5;");
                } else {
                    emailField.setStyle("-fx-border-color: #EF5350; -fx-border-width: 2; -fx-border-radius: 5;");
                }
            } else {
                emailField.setStyle("");
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleSendResetCode() {
        String email = emailField.getText().trim();

        // Validate email
        String emailError = ValidationUtil.getEmailValidationMessage(email);
        if (!emailError.isEmpty()) {
            showError(emailError);
            emailField.requestFocus();
            return;
        }

        try {
            sendCodeButton.setDisable(true);
            sendCodeButton.setText("Processing...");

            // Check if user exists
            User user = userCrud.findByEmail(email);
            if (user == null) {
                showError("No account found with this email address.");
                return;
            }

            // Generate and send reset code
            String resetCode = emailService.generateVerificationCode();
            boolean sent = emailService.sendPasswordResetEmail(email, user.getFullName(), resetCode);

            if (sent) {
                verificationManager.storePasswordResetCode(email, resetCode);

                showSuccessAlert("Password reset code has been sent to " + email + "\nCheck your email and enter the code in the next dialog.");

                // Open email verification dialog in password reset mode immediately
                openEmailVerificationDialog(user);

            } else {
                showError("Failed to send reset code. Please try again.");
            }

        } catch (Exception e) {
            showError("An error occurred. Please try again.");
            e.printStackTrace();
        } finally {
            sendCodeButton.setDisable(false);
            sendCodeButton.setText("Send Reset Code");
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    private void openEmailVerificationDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/email-verification.fxml"));
            Scene scene = new Scene(loader.load());

            EmailVerificationController controller = loader.getController();
            controller.setPendingUser(user);
            controller.setPasswordResetMode(true);

            Stage verificationStage = new Stage();
            verificationStage.setTitle("Password Reset Verification - Agrinova");
            verificationStage.setScene(scene);
            verificationStage.setResizable(false);

            // DON'T make this modal to avoid stacking issues
            // verificationStage.initModality(Modality.APPLICATION_MODAL);

            controller.setStage(verificationStage);

            // Initialize the dialog with proper timer and field setup for password reset
            controller.initializeDialog();

            // Close current stage BEFORE showing verification dialog
            stage.close();

            // Show verification dialog normally (not modal)
            verificationStage.show();

            // Bring to front and focus
            verificationStage.toFront();
            verificationStage.requestFocus();

        } catch (IOException e) {
            showError("Failed to open verification dialog.");
            e.printStackTrace();
        }
    }

    private void clearError() {
        errorLabel.setText("");
        emailField.setStyle("");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        emailField.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5;");
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
