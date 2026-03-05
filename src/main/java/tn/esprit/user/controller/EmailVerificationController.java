package tn.esprit.user.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.user.entity.Role;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.EmailService;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.VerificationCodeManager;

import java.io.IOException;

public class EmailVerificationController {
    @FXML private Label emailLabel;
    @FXML private Label instructionLabel;
    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    @FXML private Label timerLabel;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Button cancelButton;

    private User pendingUser;
    private EmailService emailService;
    private VerificationCodeManager verificationManager;
    private UserCrud userCrud;
    private Timeline countdownTimer;
    private int timeRemaining = 600; // 10 minutes in seconds
    private Stage stage;
    private Stage signupStage;
    private boolean isPasswordReset = false;

    public void setSignupStage(Stage signupStage) {
        this.signupStage = signupStage;
    }

    public void initialize() {
        emailService = new EmailService();
        verificationManager = VerificationCodeManager.getInstance();
        userCrud = new UserCrud();

        // Setup input field listener - EXACTLY like signup
        codeField.textProperty().addListener((obs, oldText, newText) -> {
            clearError();
            // Only allow digits and limit to 6 characters
            if (!newText.matches("\\d*")) {
                codeField.setText(newText.replaceAll("[^\\d]", ""));
            }
            if (codeField.getText().length() > 6) {
                codeField.setText(codeField.getText().substring(0, 6));
            }
        });

        // Simple focus setup
        Platform.runLater(() -> {
            codeField.requestFocus();
        });
    }

    public void setPendingUser(User user) {
        this.pendingUser = user;
        emailLabel.setText(user.getEmail());
    }

    public void setPasswordResetMode(boolean isPasswordReset) {
        this.isPasswordReset = isPasswordReset;

        if (isPasswordReset) {
            instructionLabel.setText("Enter the reset code sent to your email to continue with password reset:");
            verifyButton.setText("Verify Reset Code");
        } else {
            verifyButton.setText("Verify Email");
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        // Simple focus request when stage is shown
        Platform.runLater(() -> {
            codeField.requestFocus();
        });
    }

    /**
     * Call this method to fully initialize the dialog after all properties are set
     */
    public void initializeDialog() {
        // Set the correct timeout based on mode
        if (isPasswordReset) {
            timeRemaining = 900; // 15 minutes for password reset
        } else {
            timeRemaining = 600; // 10 minutes for email verification
        }

        // Start the timer
        startCountdownTimer();

        // Focus the field
        Platform.runLater(() -> {
            codeField.requestFocus();
        });
    }

    @FXML
    private void handleVerifyCode() {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showError("Please enter the verification code");
            return;
        }

        if (code.length() != 6) {
            showError("Verification code must be 6 digits");
            return;
        }

        try {
            boolean isValid;
            if (isPasswordReset) {
                isValid = verificationManager.verifyPasswordResetCode(pendingUser.getEmail(), code);
            } else {
                isValid = verificationManager.verifyEmailCode(pendingUser.getEmail(), code);
            }

            if (isValid) {
                if (isPasswordReset) {
                    // Open password reset dialog
                    openPasswordResetDialog();
                } else {
                    // Complete user registration
                    completeUserRegistration();
                }
            } else {
                showError("Invalid or expired verification code. Please try again.");
            }

        } catch (Exception e) {
            showError("Verification failed. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleResendCode() {
        try {
            resendButton.setDisable(true);
            resendButton.setText("Sending...");

            String newCode = emailService.generateVerificationCode();

            boolean sent;
            if (isPasswordReset) {
                sent = emailService.sendPasswordResetEmail(
                    pendingUser.getEmail(),
                    pendingUser.getFullName(),
                    newCode
                );
                if (sent) {
                    verificationManager.storePasswordResetCode(pendingUser.getEmail(), newCode);
                }
            } else {
                sent = emailService.sendVerificationEmail(
                    pendingUser.getEmail(),
                    pendingUser.getFullName(),
                    newCode
                );
                if (sent) {
                    verificationManager.storeVerificationCode(pendingUser.getEmail(), newCode);
                }
            }

            if (sent) {
                clearError();
                resetTimer();
                showSuccessAlert("New verification code sent successfully!");
            } else {
                showError("Failed to send verification code. Please try again.");
            }

        } catch (Exception e) {
            showError("Failed to resend verification code.");
            e.printStackTrace();
        } finally {
            resendButton.setDisable(false);
            resendButton.setText("Resend Code");
        }
    }

    @FXML
    private void handleCancel() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (isPasswordReset) {
            verificationManager.clearPasswordResetCode(pendingUser.getEmail());
        } else {
            verificationManager.clearVerificationCode(pendingUser.getEmail());
        }
        stage.close();
    }

    private void completeUserRegistration() {
        try {
            // Add user to database
            userCrud.add(pendingUser);

            // Mark email as verified
            userCrud.updateEmailVerification(pendingUser.getEmail(), true);

            showSuccessAlert("Registration completed successfully! You can now login with your credentials.");

            // Close verification dialog and redirect to login
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
            stage.close();

            // Load login scene
            Platform.runLater(() -> {
                try {
                    loadLoginScene();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            showError("Failed to complete registration. Please try again.");
            e.printStackTrace();
        }
    }

    private void openPasswordResetDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/password-reset.fxml"));
            Scene scene = new Scene(loader.load());

            PasswordResetController controller = loader.getController();
            controller.setUserEmail(pendingUser.getEmail());

            Stage resetStage = new Stage();
            resetStage.setTitle("Reset Password - Agrinova");
            resetStage.setScene(scene);
            resetStage.setResizable(false);
            resetStage.initModality(Modality.APPLICATION_MODAL);

            controller.setStage(resetStage);

            // Close current stage
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
            stage.close();

            resetStage.showAndWait();

        } catch (IOException e) {
            showError("Failed to open password reset dialog.");
            e.printStackTrace();
        }
    }

    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeRemaining--;
            updateTimerDisplay();

            if (timeRemaining <= 0) {
                countdownTimer.stop();
                handleTimeout();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void resetTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        timeRemaining = isPasswordReset ? 900 : 600; // 15 minutes for password reset, 10 for email verification
        startCountdownTimer();
    }

    private void updateTimerDisplay() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("Code expires in: %d:%02d", minutes, seconds));

        if (timeRemaining <= 60) {
            timerLabel.setStyle("-fx-text-fill: #d32f2f;");
        }
    }

    private void handleTimeout() {
        // Clean up verification code
        if (isPasswordReset) {
            verificationManager.clearPasswordResetCode(pendingUser.getEmail());
        } else {
            verificationManager.clearVerificationCode(pendingUser.getEmail());
        }

        showError("Verification code has expired. Please request a new one.");
        verifyButton.setDisable(true);
    }

    private void loadLoginScene() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
        Scene loginScene = new Scene(loader.load());
        java.net.URL css = getClass().getResource("/styles/styles.css");
        if (css != null) loginScene.getStylesheets().add(css.toExternalForm());

        // Close signup (owner stage) if present
        if (signupStage != null) {
            signupStage.close();
        }

        stage.setScene(loginScene);
        stage.setTitle("AgriNova - Login");
        stage.setResizable(true);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setWidth(1200);
        stage.setHeight(840);
        stage.centerOnScreen();
        stage.show();
    }

    private void clearError() {
        errorLabel.setText("");
        codeField.setStyle("");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        codeField.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5;");
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
