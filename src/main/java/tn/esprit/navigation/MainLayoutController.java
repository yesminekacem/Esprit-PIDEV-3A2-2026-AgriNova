package tn.esprit.navigation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import tn.esprit.user.entity.User;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import tn.esprit.user.entity.User;      // your User entity
import tn.esprit.utils.SessionManager;  // your SessionManager
import javafx.event.ActionEvent;

public class MainLayoutController {
    @FXML
    protected Button dashboardBtn;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label profileMenu;

    // When true the controller will not auto-navigate to CROPS in initialize()
    private boolean skipInitialRoute = false;

    // called by external code before FXMLLoader.load() when embedding MainLayout
    public void setSkipInitialRoute(boolean skip) {
        this.skipInitialRoute = skip;
    }

    @FXML
    public void initialize() {
        Router.init(contentArea);

        // SAFE INIT: If Crops fails, it won't break the Dashboard button anymore
        if (!skipInitialRoute) {
            try {
                Router.go(Routes.CROPS);
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Initial route (CROPS) failed: " + e.getMessage());
            }
        }

        // set profile name label from session user if available
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null) {
            profileMenu.setText(u.getFullName());
        } else {
            profileMenu.setText("My Account");
        }
    }

    @FXML
    public void openHome() {
        Router.go(Routes.CROPS);
    }

    @FXML
    public void showInventory() {
        Router.go(Routes.Inventory);
    }

    @FXML
    public void openForum() {
        Router.go(Routes.FORUM_LIST);
    }
    @FXML
    public void openMarketplace() {
        Router.go(Routes.MARKETPLACE);
    }
    @FXML
    public void openCrops() {
        Router.go(Routes.CROPS);
    }

    @FXML
    public void openrenatals() {
        Router.go(Routes.rentals);
    }

    @FXML
    public void openSettings() {
        Router.go("/fxml/user/user-dashboard.fxml");
    }

    /**
     * Logout handler wired from the topbar MenuItem.
     * Clears the session and token, then replaces the current Stage scene with the login view.
     */
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        TokenManager.clearToken();
        Stage stage = (Stage) contentArea.getScene().getWindow();
        tn.esprit.MainFX.loadLoginOnStage(stage);
    }

    // allow LoginController to request the admin dashboard be shown
    public void openAdminDashboard() {
        Router.go("/fxml/user/admin-dashboard.fxml");
    }
}
