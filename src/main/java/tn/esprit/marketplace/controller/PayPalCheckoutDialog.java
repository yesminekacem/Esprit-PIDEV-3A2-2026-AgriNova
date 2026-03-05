package tn.esprit.marketplace.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.utils.PayPalConfig;

public class PayPalCheckoutDialog {

    public enum CheckoutResult {
        APPROVED,
        CANCELLED,
        ERROR
    }

    private CheckoutResult result = CheckoutResult.ERROR;
    private Stage dialogStage;

    public CheckoutResult showAndWait(String approvalUrl) {
        System.out.println("[Dialog] Opening PayPal checkout dialog with URL: " + approvalUrl);

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Suppress directory lock warnings
        engine.setOnError(event -> {
            String errorMsg = event.getException().getMessage();
            // Ignore directory lock errors - these are just cache warnings
            if (!errorMsg.contains("DirectoryLock") && !errorMsg.contains("webview")) {
                System.out.println("[WebView] Error: " + errorMsg);
                event.getException().printStackTrace();
            }
        });

        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("PayPal Checkout");
        dialogStage.setWidth(900);
        dialogStage.setHeight(750);
        dialogStage.getIcons().setAll(tn.esprit.MainFX.getAppIcon());



        // Listen for URL changes to log user progress
        engine.locationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                System.out.println("[WebView] Location: " + newVal);

                // Detect PayPal errors
                if (newVal.contains("genericError")) {
                    System.out.println("[WebView] PayPal error page detected!");
                    System.out.println("[WebView] This usually means:");
                    System.out.println("  1. Your sandbox buyer account has no funding source");
                    System.out.println("  2. Add a test credit card to your buyer account");
                    System.out.println("  3. Or try a different buyer account");
                    System.out.println("[WebView] URL: " + newVal);
                }

                // ONLY auto-close if user explicitly navigates to return/cancel URLs
                // Don't auto-close on genericError or other PayPal pages
                if (newVal.startsWith(PayPalConfig.RETURN_URL)) {
                    System.out.println("[WebView] User approved payment (RETURN_URL detected)");
                    result = CheckoutResult.APPROVED;
                    Platform.runLater(dialogStage::close);
                } else if (newVal.startsWith(PayPalConfig.CANCEL_URL)) {
                    System.out.println("[WebView] User cancelled payment (CANCEL_URL detected)");
                    result = CheckoutResult.CANCELLED;
                    Platform.runLater(dialogStage::close);
                }
            }
        });

        // Layout with BorderPane
        BorderPane root = new BorderPane();
        root.setCenter(webView);

        // Load the PayPal approval page
        System.out.println("[WebView] Loading approval URL: " + approvalUrl);
        engine.load(approvalUrl);

        // Show dialog and wait for it to close
        dialogStage.setScene(new Scene(root));
        dialogStage.showAndWait();

        System.out.println("[Dialog] Dialog closed. Result: " + result);
        return result;
    }

    public CheckoutResult getResult() {
        return result;
    }

    public void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}

