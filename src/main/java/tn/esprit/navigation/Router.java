package tn.esprit.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class Router {
    private static StackPane contentArea;

    private Router() {}

    // call once from MainLayoutController.initialize()
    public static void init(StackPane area) {
        contentArea = area;
    }

    public static void go(String fxmlPath) {
        if (contentArea == null) {
            throw new IllegalStateException("Router not initialized. Call Router.init(contentArea) first.");
        }
        try {
            Parent view = FXMLLoader.load(Router.class.getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // When you need controller access (to pass postId), use this:
    public static <T> T goWithController(String fxmlPath) {
        if (contentArea == null) {
            throw new IllegalStateException("Router not initialized. Call Router.init(contentArea) first.");
        }
        try {
            FXMLLoader loader = new FXMLLoader(Router.class.getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            return loader.getController();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
