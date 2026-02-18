package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("=== APP STARTING ===");

        // Check if user has valid token for auto-login
        int userId = TokenManager.getUserIdFromToken();

        if (userId != -1) {
            // Valid token exists - auto login
            System.out.println("🔐 Auto-login: Loading user ID " + userId);
            try {
                UserCrud userCrud = new UserCrud();
                User user = userCrud.findById(userId);

                if (user != null) {
                    // Auto-login successful
                    SessionManager.getInstance().login(user);
                    System.out.println("✅ Auto-login successful for: " + user.getEmail());

                    // Load appropriate dashboard
                    String fxmlPath;
                    String title;

                    if (user.getRole() == tn.esprit.user.entity.Role.ADMIN) {
                        fxmlPath = "/fxml/user/admin-dashboard.fxml";
                        title = "Admin Dashboard";
                    } else {
                        fxmlPath = "/fxml/user/user-dashboard.fxml";
                        title = "User Dashboard";
                    }

                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    Scene scene = new Scene(loader.load());
                    primaryStage.setTitle("Digital Farm - " + title);
                    primaryStage.setScene(scene);
                    primaryStage.show();
                    return;
                }
            } catch (Exception e) {
                System.err.println("❌ Auto-login failed: " + e.getMessage());
                TokenManager.clearToken();
            }
        }

        // No valid token or auto-login failed - show login page
        System.out.println("🔓 No valid token - showing login page");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Digital Farm - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
