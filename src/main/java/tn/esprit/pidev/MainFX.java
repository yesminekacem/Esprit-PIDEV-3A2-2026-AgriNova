package tn.esprit.pidev;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application AgriRent
 * JavaFX + SceneBuilder
 * PIDEV 3A - 2025/2026
 */
public class MainFX extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger le fichier FXML principal avec la barre latérale
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainView.fxml")
            );
            Parent root = loader.load();

            // Créer la scène
            Scene scene = new Scene(root, 1400, 900);

            // Appliquer le CSS principal
            scene.getStylesheets().add(
                    getClass().getResource("/css/main-styles.css").toExternalForm()
            );

            // Configurer la fenêtre
            primaryStage.setTitle("AgriRent - Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            // Fermer la connexion à la base de données
            tn.esprit.pidev.utils.DatabaseConnection.getInstance().closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}