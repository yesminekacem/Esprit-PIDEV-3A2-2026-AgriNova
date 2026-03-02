package tn.esprit.user.controller;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.FaceIdService;
import tn.esprit.utils.SessionManager;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FaceSetupController {

    @FXML private ImageView webcamView;
    @FXML private ImageView capturedView;
    @FXML private Label statusLabel;
    @FXML private Label countdownLabel;
    @FXML private Label faceIdStatusLabel;
    @FXML private Button captureButton;
    @FXML private Button saveButton;
    @FXML private Button removeButton;
    @FXML private VBox capturedBox;
    @FXML private ProgressBar saveProgress;

    private Webcam webcam;
    private ScheduledExecutorService feedExecutor;
    private ScheduledExecutorService countdownExecutor;
    private BufferedImage capturedFrame;
    private final UserCrud userCrud = new UserCrud();

    @FXML
    public void initialize() {
        saveButton.setDisable(true);
        saveProgress.setVisible(false);
        capturedBox.setVisible(false);
        capturedBox.setManaged(false);
        refreshStatus(SessionManager.getInstance().getCurrentUser());
        startWebcam();
    }

    private void startWebcam() {
        new Thread(() -> {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> setStatus("⚠ No webcam detected.", "red"));
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
        }, "webcam-init").start();
    }

    public void stopWebcam() {
        if (feedExecutor != null) feedExecutor.shutdownNow();
        if (countdownExecutor != null) countdownExecutor.shutdownNow();
        if (webcam != null && webcam.isOpen()) webcam.close();
    }

    @FXML
    private void handleCapture() {
        if (webcam == null || !webcam.isOpen()) { setStatus("⚠ Webcam not available.", "red"); return; }
        captureButton.setDisable(true);
        countdownLabel.setVisible(true);
        AtomicInteger secs = new AtomicInteger(3);
        countdownLabel.setText("3");
        countdownExecutor = Executors.newSingleThreadScheduledExecutor();
        countdownExecutor.scheduleAtFixedRate(() -> {
            int v = secs.decrementAndGet();
            Platform.runLater(() -> {
                if (v > 0) {
                    countdownLabel.setText(String.valueOf(v));
                } else {
                    countdownLabel.setVisible(false);
                    countdownExecutor.shutdownNow();
                    capturedFrame = webcam.getImage();
                    if (capturedFrame != null) {
                        capturedView.setImage(SwingFXUtils.toFXImage(capturedFrame, null));
                        capturedBox.setVisible(true);
                        capturedBox.setManaged(true);
                        saveButton.setDisable(false);
                        setStatus("📸 Captured! Click Save to register.", "#1565C0");
                    }
                    captureButton.setDisable(false);
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    @FXML
    private void handleSave() {
        if (capturedFrame == null) { setStatus("⚠ Capture first.", "red"); return; }
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) return;
        captureButton.setDisable(true);
        saveButton.setDisable(true);
        saveProgress.setVisible(true);
        saveProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        setStatus("⏳ Saving…", "#1565C0");
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                current.setFaceData(FaceIdService.toBase64(capturedFrame));
                userCrud.update(current);
                SessionManager.getInstance().login(userCrud.findById(current.getId()));
                return null;
            }
        };
        t.setOnSucceeded(e -> {
            saveProgress.setVisible(false);
            captureButton.setDisable(false);
            refreshStatus(SessionManager.getInstance().getCurrentUser());
            setStatus("✔  Face ID registered!", "#16a34a");
        });
        t.setOnFailed(e -> {
            saveProgress.setVisible(false);
            captureButton.setDisable(false);
            saveButton.setDisable(false);
            setStatus("✖ " + t.getException().getMessage(), "red");
        });
        new Thread(t, "face-save").start();
    }

    @FXML
    private void handleRemove() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current == null) return;
        try {
            current.setFaceData(null);
            userCrud.update(current);
            SessionManager.getInstance().login(userCrud.findById(current.getId()));
            capturedFrame = null;
            capturedView.setImage(null);
            capturedBox.setVisible(false);
            capturedBox.setManaged(false);
            saveButton.setDisable(true);
            refreshStatus(SessionManager.getInstance().getCurrentUser());
            setStatus("✔  Face ID removed.", "#16a34a");
        } catch (SQLException ex) { setStatus("✖ " + ex.getMessage(), "red"); }
    }

    public void onClose() { stopWebcam(); }

    private void refreshStatus(User user) {
        if (faceIdStatusLabel == null) return;
        boolean has = user != null && user.getFaceData() != null && !user.getFaceData().isEmpty();
        faceIdStatusLabel.setText(has ? "✔  Registered" : "Not set up");
        faceIdStatusLabel.setStyle("-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:"
                + (has ? "#16a34a" : "#9ca3af") + ";");
    }

    private void setStatus(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
    }
}

