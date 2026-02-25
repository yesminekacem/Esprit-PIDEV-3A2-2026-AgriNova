package tn.esprit.crop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CropApp extends Application {



    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/layout/mainlayout.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root);

        // Add CSS
        scene.getStylesheets().add(
                getClass().getResource("/styles/styles.css").toExternalForm()
        );
        scene.getStylesheets().add(
                getClass().getResource("/styles/crop.css").toExternalForm()
        );

        stage.setTitle("Digital Farm");
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
