package tn.esprit.navigation;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        Router.init(contentArea);
        Router.go(Routes.HOME);
    }

    @FXML
    private void openHome() {
        Router.go(Routes.HOME);
    }

    @FXML
    private void openForum() {
        Router.go(Routes.FORUM_LIST);
    }
    @FXML
    private void openMarketplace() {
        Router.go(Routes.MARKETPLACE);
    }
    private void openCrops() {
        Router.go(Routes.CROPS);
    }
}
