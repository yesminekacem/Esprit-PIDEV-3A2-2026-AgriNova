package tn.esprit;

import javafx.fxml.FXMLLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.IOException;

public class MainFX extends Application {

    private static Image appIcon;

    public static Image getAppIcon() {
        if (appIcon == null) {
            appIcon = new Image(MainFX.class.getResourceAsStream("/logo.png"));
        }
        return appIcon;
    }

    /** @deprecated Use getAppIcon() instead */
    public static javafx.scene.image.WritableImage createLeafIcon() {
        return null;
    }

    /**
     * Swaps the given stage to the login scene, preserving its
     * maximized / windowed state exactly.
     */
    public static void loadLoginOnStage(Stage stage) {
        try {
            boolean wasMaximized = stage.isMaximized();

            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("/fxml/user/login.fxml"));
            Scene scene = new Scene(loader.load());
            java.net.URL css = MainFX.class.getResource("/styles/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            stage.setTitle("AgriNova - Login");
            // Un-maximize first so setWidth/setHeight take effect when windowed
            stage.setMaximized(false);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.getIcons().setAll(getAppIcon());

            if (wasMaximized) {
                stage.setMaximized(true);
            } else {
                stage.setWidth(1200);
                stage.setHeight(840);
                stage.centerOnScreen();
            }
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("=== APP STARTING ===");
        primaryStage.getIcons().add(getAppIcon());

        // Check if user has valid token for auto-login
        int userId = TokenManager.getUserIdFromToken();

        if (userId != -1) {
            System.out.println("🔐 Auto-login: Loading user ID " + userId);
            try {
                UserCrud userCrud = new UserCrud();
                User user = userCrud.findById(userId);

                if (user != null) {
                    SessionManager.getInstance().login(user);
                    System.out.println("✅ Auto-login successful for: " + user.getEmail());

                    String fxmlPath;
                    String title;
                    if (user.getRole() == tn.esprit.user.entity.Role.ADMIN) {
                        fxmlPath = "/fxml/user/admin-dashboard.fxml";
                        title = "Admin Dashboard";
                    } else {
                        fxmlPath = "/fxml/layout/MainLayout.fxml";
                        title = "Dashboard";
                    }

                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    Scene scene = new Scene(loader.load());
                    java.net.URL css = getClass().getResource("/styles/styles.css");
                    if (css != null) scene.getStylesheets().add(css.toExternalForm());

                    primaryStage.setTitle("AgriNova - " + title);
                    primaryStage.setScene(scene);
                    primaryStage.setResizable(true);
                    primaryStage.setMinWidth(1000);
                    primaryStage.setMinHeight(700);
                    primaryStage.setMaximized(true);
                    primaryStage.show();
                    return;
                }
            } catch (Exception e) {
                System.err.println("❌ Auto-login failed: " + e.getMessage());
                TokenManager.clearToken();
            }
        }

        // No valid token — show login page
        System.out.println("🔓 No valid token - showing login page");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
        Scene scene = new Scene(loader.load());
        java.net.URL css = getClass().getResource("/styles/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        primaryStage.setTitle("AgriNova - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(840);
        primaryStage.setMaximized(false);
        primaryStage.centerOnScreen();
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
