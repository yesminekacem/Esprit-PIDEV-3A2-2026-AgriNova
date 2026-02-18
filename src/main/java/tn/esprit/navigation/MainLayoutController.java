package tn.esprit.navigation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class MainLayoutController {

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        loadView("/fxml/layout/Home.fxml"); // ✅ default view
    }

    @FXML private void openDashboard()   { loadView("/fxml/layout/Home.fxml"); }
    @FXML private void openMarketplace() { loadView("/fxml/marketplace/MarketplaceView.fxml"); }
    @FXML private void openCart()        { loadView("/fxml/marketplace/CartView.fxml"); }
    @FXML private void openOrders()      { loadView("/fxml/marketplace/OrderView.fxml"); }

    // ⚠️ These don't have FXML yet — they print a message instead of crashing
    @FXML private void openUsers()     { System.out.println("Users view not yet created"); }
    @FXML private void openCrops()     { System.out.println("Crops view not yet created"); }
    @FXML private void openInventory() { System.out.println("Inventory view not yet created"); }
    @FXML private void openForum()     { System.out.println("Forum view not yet created"); }
    @FXML private void openSettings()  { System.out.println("Settings view not yet created"); }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("Failed to load: " + fxmlPath + " — " + e.getMessage());
        }
    }
}
