package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/layout/MainLayout.fxml")
        );

        Scene scene = new Scene(loader.load(), 1000, 700);
        stage.setTitle("Digital Farm Management");
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/forum.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
