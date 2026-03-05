package tn.esprit.user.controller;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.FaceIdService;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FaceLoginController {

    @FXML private ImageView webcamView;
    @FXML private Label statusLabel;
    @FXML private Label resultLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button verifyButton;
    @FXML private Button cancelButton;

    private Webcam webcam;
    private ScheduledExecutorService feedExecutor;
    private Stage stage;
    private final UserCrud userCrud = new UserCrud();

    public void setStage(Stage s) {
        this.stage = s;
        s.setOnCloseRequest(e -> stopWebcam());
    }

    @FXML
    public void initialize() {
        progressBar.setVisible(false);
        startWebcam();
    }

    private void startWebcam() {
        new Thread(() -> {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> { statusLabel.setText("⚠ No webcam."); verifyButton.setDisable(true); });
                return;
            }
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open();
            feedExecutor = Executors.newSingleThreadScheduledExecutor();
            feedExecutor.scheduleAtFixedRate(() -> {
                if (webcam != null && webcam.isOpen()) {
                    BufferedImage f = webcam.getImage();
                    if (f != null) Platform.runLater(() -> webcamView.setImage(SwingFXUtils.toFXImage(f, null)));
                }
            }, 0, 40, TimeUnit.MILLISECONDS);
        }, "face-login-webcam").start();
    }

    private void stopWebcam() {
        if (feedExecutor != null) feedExecutor.shutdownNow();
        if (webcam != null && webcam.isOpen()) webcam.close();
    }

    @FXML
    private void handleVerify() {
        if (webcam == null || !webcam.isOpen()) { showResult("⚠ Webcam not ready.", "red"); return; }
        BufferedImage live = webcam.getImage();
        if (live == null) { showResult("⚠ Could not capture frame.", "red"); return; }

        verifyButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("⏳ Verifying…");

        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                List<User> all = userCrud.findAll();
                for (User u : all) {
                    if (u.getFaceData() == null || u.getFaceData().isEmpty()) continue;
                    if (!u.isEmailVerified()) continue;
                    if (FaceIdService.verify(u.getFaceData(), live)) return u;
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            verifyButton.setDisable(false);
            User matched = task.getValue();
            if (matched != null) {
                statusLabel.setText("✔ Recognised!");
                showResult("✔  Welcome, " + matched.getFullName() + "!", "#16a34a");
                new Thread(() -> {
                    try { Thread.sleep(700); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> proceedToApp(matched));
                }).start();
            } else {
                statusLabel.setText("✖ Not recognised");
                showResult("✖  Face not recognised. Try again or use password.", "red");
            }
        });
        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            verifyButton.setDisable(false);
            showResult("✖ Error: " + task.getException().getMessage(), "red");
        });
        new Thread(task, "face-verify").start();
    }

    @FXML
    private void handleCancel() { stopWebcam(); if (stage != null) stage.close(); }

    private void proceedToApp(User user) {
        stopWebcam();
        SessionManager.getInstance().login(user);
        TokenManager.saveToken(user.getId());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
            Scene scene = new Scene(loader.load());
            URL css = getClass().getResource("/styles/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            Stage main = new Stage();
            main.setTitle("AgriNova");
            main.setScene(scene);
            main.setMinWidth(1200);
            main.setMinHeight(700);
            main.setMaximized(true);
            main.show();
            if (stage != null) stage.close();
        } catch (Exception ex) { showResult("✖ " + ex.getMessage(), "red"); }
    }

    private void showResult(String msg, String color) {
        resultLabel.setText(msg);
        resultLabel.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
    }
}

