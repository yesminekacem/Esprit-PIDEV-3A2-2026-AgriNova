package tn.esprit.navigation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import tn.esprit.user.entity.User;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;

    @FXML
    private MenuButton profileMenu;

    // When true the controller will not auto-navigate to CROPS in initialize()
    private boolean skipInitialRoute = false;

    // called by external code before FXMLLoader.load() when embedding MainLayout
    public void setSkipInitialRoute(boolean skip) {
        this.skipInitialRoute = skip;
    }

    @FXML
    public void initialize() {
        Router.init(contentArea);
        // ensure the first shown page is the Crops page unless we're asked to skip it
        if (!skipInitialRoute) {
            Router.go(Routes.CROPS);
        }

        // set profile menu text from session user if available
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null) {
            profileMenu.setText(u.getFullName() + " ▾");
        } else {
            profileMenu.setText("Account ▾");
        }
    }

    @FXML
    private void openHome() {
        Router.go(Routes.CROPS);
    }

    @FXML
    private void openForum() {
        Router.go(Routes.FORUM_LIST);
    }
    @FXML
    private void openMarketplace() {
        Router.go(Routes.MARKETPLACE);
    }
    @FXML
    private void openCrops() {
        Router.go(Routes.CROPS);
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
        // clear session and persistent token
        SessionManager.getInstance().logout();
        TokenManager.clearToken();

        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            // ensure stylesheet fallback
            java.net.URL css = getClass().getResource("/styles/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("Digital Farm - Login");
            stage.setScene(scene);
            stage.setMinWidth(1000); // ✅ minimum window size
            stage.setMinHeight(700);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // no UI alert here to keep handler simple; controller-level handlers show errors elsewhere
        }
    }

    // allow LoginController to request the admin dashboard be shown
    public void openAdminDashboard() {
        Router.go("/fxml/user/admin-dashboard.fxml");
    }
}
