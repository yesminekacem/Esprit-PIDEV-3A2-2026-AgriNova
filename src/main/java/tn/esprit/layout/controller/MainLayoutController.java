package tn.esprit.layout.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class MainLayoutController {

    @FXML
    private StackPane centerContent;

    private void loadPage(String path) {
        try {
            Node page = FXMLLoader.load(getClass().getResource(path));
            centerContent.getChildren().clear();
            centerContent.getChildren().add(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenCrops() {
        loadPage("/fxml/crop/CropsPage.fxml");
    }

    @FXML
    private void handleOpenDashboard() {
        centerContent.getChildren().clear();
        centerContent.getChildren().add(new Label("Dashboard Page"));
    }

    @FXML
    private void handleOpenUsers() {
        centerContent.getChildren().clear();
        centerContent.getChildren().add(new Label("Users Page"));
    }

    @FXML
    private void handleOpenInventory() {
        centerContent.getChildren().clear();
        centerContent.getChildren().add(new Label("Inventory Page"));
    }

    @FXML
    private void handleOpenMarketplace() {
        centerContent.getChildren().clear();
        centerContent.getChildren().add(new Label("Marketplace Page"));
    }

    @FXML
    private void handleOpenForum() {
        centerContent.getChildren().clear();
        centerContent.getChildren().add(new Label("Forum Page"));
    }
}
