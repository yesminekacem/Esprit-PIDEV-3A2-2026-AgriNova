package tn.esprit.navigation;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;

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
            URL url = Router.class.getResource(fxmlPath);
            if (url == null) {
                throw new RuntimeException("FXML not found on classpath: " + fxmlPath);
            }
            Parent view = FXMLLoader.load(url);
            contentArea.getChildren().setAll(view);

            // Make Region-based views fill the content area
            if (view instanceof Region) {
                Region r = (Region) view;
                r.setMaxWidth(Double.MAX_VALUE);
                r.setMaxHeight(Double.MAX_VALUE);
                r.prefWidthProperty().bind(contentArea.widthProperty());
                r.prefHeightProperty().bind(contentArea.heightProperty());
            }
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
            URL url = Router.class.getResource(fxmlPath);
            if (url == null) {
                throw new RuntimeException("FXML not found on classpath: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);

            // Make Region-based views fill the content area
            if (view instanceof Region) {
                Region r = (Region) view;
                r.setMaxWidth(Double.MAX_VALUE);
                r.setMaxHeight(Double.MAX_VALUE);
                r.prefWidthProperty().bind(contentArea.widthProperty());
                r.prefHeightProperty().bind(contentArea.heightProperty());
            }

            return loader.getController();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}