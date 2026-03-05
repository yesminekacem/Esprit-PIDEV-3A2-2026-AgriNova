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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.FaceIdService;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FaceLoginController {

    @FXML private ImageView webcamView;
    @FXML private Label statusLabel;
    @FXML private Label resultLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button cancelButton;
    @FXML private VBox accountPickerBox;

    private Webcam webcam;
    private ScheduledExecutorService feedExecutor;
    private ScheduledExecutorService scanExecutor;
    private Stage stage;
    private final UserCrud userCrud = new UserCrud();
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final AtomicBoolean verifying = new AtomicBoolean(false);

    public void setStage(Stage s) {
        this.stage = s;
        s.setOnCloseRequest(e -> stopAll());
    }

    @FXML
    public void initialize() {
        progressBar.setVisible(false);
        if (accountPickerBox != null) {
            accountPickerBox.setVisible(false);
            accountPickerBox.setManaged(false);
        }
        startWebcam();
    }

    private void startWebcam() {
        new Thread(() -> {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> statusLabel.setText("⚠ No webcam found."));
                return;
            }
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open();

            feedExecutor = Executors.newSingleThreadScheduledExecutor();
            feedExecutor.scheduleAtFixedRate(() -> {
                if (webcam != null && webcam.isOpen()) {
                    BufferedImage f = webcam.getImage();
                    if (f != null) Platform.runLater(() ->
                            webcamView.setImage(SwingFXUtils.toFXImage(f, null)));
                }
            }, 0, 40, TimeUnit.MILLISECONDS);

            scanning.set(true);
            scanExecutor = Executors.newSingleThreadScheduledExecutor();
            scanExecutor.scheduleAtFixedRate(() -> {
                if (!scanning.get() || verifying.get()) return;
                if (webcam == null || !webcam.isOpen()) return;
                BufferedImage frame = webcam.getImage();
                if (frame == null) return;
                triggerAutoScan(frame);
            }, 1500, 1500, TimeUnit.MILLISECONDS);

            Platform.runLater(() -> statusLabel.setText("👁  Looking for your face…"));
        }, "face-login-webcam").start();
    }

    private void triggerAutoScan(BufferedImage frame) {
        if (!verifying.compareAndSet(false, true)) return;

        Platform.runLater(() -> {
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            statusLabel.setText("⏳ Verifying…");
        });

        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                List<User> matches = new ArrayList<>();
                for (User u : userCrud.findAll()) {
                    if (u.getFaceData() == null || u.getFaceData().isEmpty()) continue;
                    if (!u.isEmailVerified()) continue;
                    if (FaceIdService.verify(u.getFaceData(), frame)) matches.add(u);
                }
                return matches;
            }
        };

        task.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            verifying.set(false);
            List<User> matches = task.getValue();

            if (matches.isEmpty()) {
                statusLabel.setText("👁  Looking for your face…");
                showResult("", "#374151");
            } else if (matches.size() == 1) {
                scanning.set(false);
                User matched = matches.get(0);
                statusLabel.setText("✔  Recognised!");
                showResult("✔  Welcome back, " + matched.getFullName() + "!", "#16a34a");
                new Thread(() -> {
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> proceedToApp(matched));
                }).start();
            } else {
                scanning.set(false);
                statusLabel.setText("✔  Choose your account:");
                showResult("", "#374151");
                showAccountPicker(matches);
            }
        });

        task.setOnFailed(e -> {
            progressBar.setVisible(false);
            verifying.set(false);
            statusLabel.setText("👁  Looking for your face…");
        });

        new Thread(task, "face-verify").start();
    }

    private void showAccountPicker(List<User> matches) {
        if (accountPickerBox == null) return;
        accountPickerBox.getChildren().clear();

        Label heading = new Label("Multiple accounts found — select yours:");
        heading.setStyle("-fx-font-size:13px;-fx-font-weight:800;-fx-text-fill:#374151;");
        accountPickerBox.getChildren().add(heading);

        for (User u : matches) {
            Button btn = new Button("👤  " + u.getFullName() + "   ·   " + u.getEmail());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle(
                "-fx-background-color:#2E7D32;-fx-text-fill:white;-fx-background-radius:10;" +
                "-fx-padding:11 20;-fx-cursor:hand;-fx-font-size:13px;-fx-font-weight:700;" +
                "-fx-effect:dropshadow(gaussian,rgba(46,125,50,0.25),8,0.2,0,2);"
            );
            btn.setOnAction(ev -> proceedToApp(u));
            accountPickerBox.getChildren().add(btn);
        }

        accountPickerBox.setVisible(true);
        accountPickerBox.setManaged(true);

        // Resize stage to fit content
        if (stage != null) Platform.runLater(() -> stage.sizeToScene());
    }

    @FXML
    private void handleCancel() { stopAll(); if (stage != null) stage.close(); }

    private void stopAll() {
        scanning.set(false);
        if (scanExecutor != null) scanExecutor.shutdownNow();
        if (feedExecutor != null) feedExecutor.shutdownNow();
        if (webcam != null && webcam.isOpen()) webcam.close();
    }

    private void proceedToApp(User user) {
        stopAll();
        SessionManager.getInstance().login(user);
        TokenManager.saveToken(user.getId());
        try {
            boolean isAdmin = user.getRole() == tn.esprit.user.entity.Role.ADMIN;
            String fxmlPath = isAdmin ? "/fxml/user/admin-dashboard.fxml" : "/fxml/layout/MainLayout.fxml";
            String title    = isAdmin ? "AgriNova - Admin Panel" : "AgriNova";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            URL css = getClass().getResource("/styles/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            Stage main = new Stage();
            main.setTitle(title);
            main.setScene(scene);
            main.setMinWidth(1200);
            main.setMinHeight(700);
            main.setMaximized(true);
            main.getIcons().setAll(tn.esprit.MainFX.getAppIcon());
            main.show();
            if (stage != null) stage.close();
        } catch (Exception ex) { showResult("✖ " + ex.getMessage(), "red"); }
    }

    private void showResult(String msg, String color) {
        if (resultLabel == null) return;
        resultLabel.setText(msg);
        resultLabel.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
    }
}

